package com.example.room;

import com.example.room.dto.DtoCreateRoom;
import com.example.room.dto.DtoJoinRoom;
import com.example.room.entity.Room;
import com.example.room.repository.RoomRepository;
import com.example.room.service.QueueService;
import com.example.room.service.RoomService;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.ListTopicsResult;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Testcontainers
public class IntegrationnTests {

    @Container
    private static MongoDBContainer mongoDBContainer = new MongoDBContainer("mongo:7.0");
    @Container
    private static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );
    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.kafka.bootstrap-servers", kafkaContainer::getBootstrapServers);
        registry.add("kafka_host", kafkaContainer::getBootstrapServers);
        registry.add("spring.data.mongodb.uri", mongoDBContainer::getReplicaSetUrl);
    }
    @Autowired
    private RoomService roomService;
    @Autowired
    private RoomRepository roomRepository;
    @Autowired
    private AdminClient adminClient;
    //private KafkaProducer<String, DtoJoinRoom> kafkaProducer;
    @MockBean
    private KafkaTemplate<String, DtoJoinRoom> kafkaUserJoinedRoom;
    @MockBean
    private QueueService queueService;

    @Test
    public void createRoom() throws ExecutionException, InterruptedException {

        DtoCreateRoom dto = new DtoCreateRoom();
        dto.setName("Room1");
        dto.setMaxCapacity(30);
        dto.setDuration(10);

        Room room1 = new Room();
        room1.setId("roomId123");
        room1.setName(dto.getName());
        room1.setMaxCapacity(dto.getMaxCapacity());
        room1.setDuration(dto.getDuration());
        room1.setPaused(false);
        room1.setUsers(new ArrayList<>());

        roomService.createRoom(dto);

        Optional<Room> savedRoom = roomRepository.findByName(room1.getName());
        assertThat(savedRoom).isPresent();
        assertThat(savedRoom.get().getName()).isEqualTo("Room1");

        ListTopicsResult topicsResult = adminClient.listTopics();
        Set<String> topics = topicsResult.names().get();
        assertTrue(topics.stream().anyMatch(topic -> topic.equals(savedRoom.get().getQueueKafkaTopic())));
    }

    @Test
    public void consumeNewUserEvent() {

        Properties props = new Properties();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaContainer.getBootstrapServers());
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class.getName());
        props.put(JsonSerializer.TYPE_MAPPINGS, "newUser:com.example.room.dto.DtoJoinRoom");

        KafkaProducer<String, DtoJoinRoom> kafkaProducer = new KafkaProducer<>(props);

        DtoCreateRoom dto = new DtoCreateRoom();
        dto.setName("Room2");
        dto.setMaxCapacity(30);
        dto.setDuration(10);

        Room room1 = new Room();
        room1.setName(dto.getName());
        room1.setMaxCapacity(dto.getMaxCapacity());
        room1.setDuration(dto.getDuration());
        room1.setPaused(false);
        room1.setUsers(new ArrayList<>());

        roomService.createRoom(dto);

        Optional<Room> savedRoom = roomRepository.findByName(room1.getName());
        assertThat(savedRoom).isPresent();
        Room room2 = savedRoom.get();

        String userEmail = "example@email.com";
        DtoJoinRoom dtoJoinRoom = new DtoJoinRoom();
        dtoJoinRoom.setRoomId(room2.getId());
        dtoJoinRoom.setName(dto.getName());
        dtoJoinRoom.setEmail(userEmail);

        ProducerRecord<String, DtoJoinRoom> record = new ProducerRecord<>(room2.getQueueKafkaTopic(), dtoJoinRoom);

        kafkaProducer.send(record);

        await()
        .pollInterval(Duration.ofSeconds(5))
        .untilAsserted(() -> {

            Room room3 = roomRepository.findByName(room1.getName()).get();
            assertTrue(room3.getUsers().stream().anyMatch(user -> user.getEmail().equals(userEmail)));

        });

    }
}


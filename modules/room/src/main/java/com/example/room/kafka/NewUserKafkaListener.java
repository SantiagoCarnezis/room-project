package com.example.room.kafka;

import com.example.room.dto.DtoJoinRoom;
import com.example.room.entity.Room;
import com.example.room.entity.User;
import com.example.room.exception.RoomNotFoundException;
import com.example.room.repository.RoomRepository;
import com.example.room.task.RoomCleanupTask;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.listener.AcknowledgingConsumerAwareMessageListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.locks.ReentrantLock;


@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class NewUserKafkaListener implements AcknowledgingConsumerAwareMessageListener<String, DtoJoinRoom> {

    private final RoomRepository roomRepository;
    private final KafkaTopicService kafkaTopicService;
    private final CustomKafkaContainerRegistry customKafkaContainerRegistry;
    private final ReentrantLock lock = new ReentrantLock();
    private final MeterRegistry meterRegistry;
    private final Logger logger = LoggerFactory.getLogger(NewUserKafkaListener.class);

    @Override
    public void onMessage(ConsumerRecord<String, DtoJoinRoom> data, Acknowledgment ack, Consumer<?, ?> consumer) {


        DtoJoinRoom dto = data.value();

        logger.info("Starting Thread: {} | Partition: {} | User: {}",
                Thread.currentThread().getName(), data.partition(), dto.getEmail());

        lock.lock();
        Room room = getRoom(dto.getRoomId());

        logger.info("Processing user {}", dto.getEmail());

        if (room.isFull()) {

            logger.warn("user {} could not join room {} because it is full", dto.getEmail(), dto.getRoomId());
            customKafkaContainerRegistry.stopListener(room.getId());
            this.revertOffset(consumer, data.topic(), data.partition());
        }
        else {

            this.joinRoom(room, dto);

            ack.acknowledge();

            if(room.isFull()) {

                logger.warn("Room {} is full", room.getId());
                customKafkaContainerRegistry.stopListener(room.getId());

                Timer timer = new Timer();

                TimerTask task = new RoomCleanupTask(room.getId(), roomRepository, customKafkaContainerRegistry, timer);

                timer.schedule(task, room.getDuration() * 1000L);

            }

            kafkaTopicService.produceUserJoinedRoom(dto);
        }

        recordConsumptionLatency(data);

        lock.unlock();
    }

    private void revertOffset(Consumer<?, ?> consumer, String topic, int partition) {

        TopicPartition topicPartition = new TopicPartition(topic, partition);

        consumer.seek(topicPartition, consumer.position(topicPartition) - 1);
    }


    private void joinRoom(Room room, DtoJoinRoom dto) {

        User user = new User();
        user.setEmail(dto.getEmail());
        user.setName(dto.getName());

        room.addUser(user);

        logger.info("adding user {} to room {}. Current capacity: {}/{}",
                user.getEmail(), room.getId(), room.getUsers().size(), room.getMaxCapacity());

        roomRepository.save(room);
    }

    private Room getRoom(String id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }

    private void recordConsumptionLatency(ConsumerRecord<String, DtoJoinRoom> data) {

        long latency = System.currentTimeMillis() - data.timestamp();

        meterRegistry.timer("kafka.message.latency",
                        "topic", data.topic(),
                        "partition", String.valueOf(data.partition()))
                .record(Duration.ofMillis(latency));
    }
}

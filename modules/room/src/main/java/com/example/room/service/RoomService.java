package com.example.room.service;

import com.example.room.config.CustomKafkaListenerFactory;
import com.example.room.dto.DtoCreateRoom;
import com.example.room.entity.Room;
import com.example.room.exception.RoomNotFoundException;
import com.example.room.kafka.CustomKafkaContainerRegistry;
import com.example.room.kafka.KafkaTopicService;
import com.example.room.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class RoomService {

    private final KafkaTopicService kafkaTopicService;
    private final CustomKafkaListenerFactory customKafkaListenerFactory;
    private final CustomKafkaContainerRegistry customKafkaContainerRegistry;
    private final RoomRepository roomRepository;
    private final QueueService queueService;
    private final Logger logger = LoggerFactory.getLogger(RoomService.class);

    public Room createRoom(DtoCreateRoom dto) {

        Room room = new Room();
        room.setUsers(new ArrayList<>());
        room.setName(dto.getName());
        room.setMaxCapacity(dto.getMaxCapacity());
        room.setDuration(dto.getDuration());
        room = roomRepository.save(room);

        String queueKafkaTopic = kafkaTopicService.createTopic(room.getId());
        customKafkaListenerFactory.createListener(queueKafkaTopic, room.getId());

        room.setQueueKafkaTopic(queueKafkaTopic);
        room = roomRepository.save(room);

        logger.info("Room created {}", room.getId());

        queueService.createQueue(room);

        return room;
    }

    public void deleteRoom(String roomId) {

        roomRepository.deleteById(roomId);
        logger.info("Room deleted {}", roomId);

        kafkaTopicService.deleteTopic(roomId);
        customKafkaContainerRegistry.deleteListener(roomId);
        queueService.deleteQueue(roomId);
    }

    public void pause(String roomId)
    {
        Room room = getRoom(roomId);
        room.setPaused(true);
        roomRepository.save(room);

        customKafkaContainerRegistry.stopListener(roomId);
    }

    public void resume(String roomId)
    {
        Room room = getRoom(roomId);
        room.setPaused(false);
        roomRepository.save(room);

        customKafkaContainerRegistry.resumeListener(roomId);
    }

    public void resumeAllRooms()
    {
        List<Room> rooms = roomRepository.findAll();

        for (Room room:rooms) {

            room.setPaused(false);
            roomRepository.save(room);
            customKafkaContainerRegistry.resumeListener(room.getId());
        }
    }

    public List<Room> getAllRooms() {
        return roomRepository.findAll();
    }

    private Room getRoom(String id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }
}


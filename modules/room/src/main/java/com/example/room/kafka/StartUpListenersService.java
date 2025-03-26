package com.example.room.kafka;

import com.example.room.config.CustomKafkaListenerFactory;
import com.example.room.entity.Room;
import com.example.room.repository.RoomRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class StartUpListenersService {

    private final RoomRepository roomRepository;
    private final CustomKafkaListenerFactory customKafkaListenerFactory;
    private final Logger logger = LoggerFactory.getLogger(StartUpListenersService.class);

    @PostConstruct
    public void recreateListeners() {

        logger.info("Recreating rooms...");

        List<Room> rooms = roomRepository.findAll();

        rooms.forEach(room ->
                customKafkaListenerFactory.createListener(room.getQueueKafkaTopic(), room.getId(), !room.isPaused()));
    }
}

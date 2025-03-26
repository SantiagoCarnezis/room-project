package com.example.room.task;

import com.example.room.entity.Room;
import com.example.room.exception.RoomNotFoundException;
import com.example.room.kafka.CustomKafkaContainerRegistry;
import com.example.room.repository.RoomRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Timer;
import java.util.TimerTask;

public class RoomCleanupTask extends TimerTask {

    private final String roomId;
    private final RoomRepository roomRepository;
    private final CustomKafkaContainerRegistry customKafkaContainerRegistry;
    private final Timer timer;
    private final Logger logger = LoggerFactory.getLogger(RoomCleanupTask.class);

    public RoomCleanupTask(String roomId, RoomRepository roomRepository,
                           CustomKafkaContainerRegistry customKafkaContainerRegistry, Timer timer) {

        this.roomId = roomId;
        this.roomRepository = roomRepository;
        this.customKafkaContainerRegistry = customKafkaContainerRegistry;
        this.timer = timer;
    }

    @Override
    public void run() {

        Room room = this.getRoom(roomId);
        logger.warn("Cleaning room {} after {} seconds", room.getId(), room.getDuration());
        room.removeAll();
        roomRepository.save(room);

        if (!room.isPaused()) {
            customKafkaContainerRegistry.resumeListener(roomId);
        }

        timer.cancel();
    }

    private Room getRoom(String id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new RoomNotFoundException(id));
    }
}


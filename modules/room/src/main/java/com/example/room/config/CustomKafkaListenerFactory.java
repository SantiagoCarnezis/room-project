package com.example.room.config;

import com.example.room.dto.DtoJoinRoom;
import com.example.room.kafka.CustomKafkaContainerRegistry;
import com.example.room.kafka.CustomRebalanceListener;
import com.example.room.kafka.NewUserKafkaListener;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class CustomKafkaListenerFactory {

    private final NewUserKafkaListener newUserKafkaListener;
    private final ConcurrentKafkaListenerContainerFactory<String, DtoJoinRoom> factory;
    private final CustomKafkaContainerRegistry customKafkaContainerRegistry;
    private final CustomRebalanceListener rebalanceListener;
    private final Logger logger = LoggerFactory.getLogger(CustomKafkaListenerFactory.class);

    public String createListener(String topic, String roomId) {

        return this.createListener(topic, roomId, true);
    }

    public String createListener(String topic, String roomId, boolean startsImmediately) {

        ConcurrentMessageListenerContainer<String, DtoJoinRoom> container =
                this.createContainer(topic, roomId, startsImmediately);

        customKafkaContainerRegistry.registerContainer(container, roomId);

        return roomId;
    }

    private ConcurrentMessageListenerContainer<String, DtoJoinRoom> createContainer(
            String topic, String roomId, boolean startsImmediately) {

        ConcurrentMessageListenerContainer<String, DtoJoinRoom> container = factory.createContainer(topic);
        container.getContainerProperties().setMessageListener(newUserKafkaListener);
        container.getContainerProperties().setGroupId("new-user" + roomId + "-group");
        container.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        container.getContainerProperties().setConsumerRebalanceListener(rebalanceListener);
        container.setBeanName(roomId);

        logger.info("Creating container for room {}", roomId);

        if (startsImmediately) {
            container.start();
            logger.info("Starting container for room {}", roomId);
        }

        return container;
    }
}

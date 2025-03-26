package com.example.queue.kafka;

import com.example.queue.dto.*;
import com.example.queue.entity.Queue;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class KafkaProducerService {

    private final KafkaTemplate<String, DtoJoinRoom> newUserProducer;
    private final Logger logger = LoggerFactory.getLogger(KafkaProducerService.class);

    public void produceNewUserEvent(DtoJoinQueue dto, Queue queue) {

        DtoJoinRoom dtoJoinRoom = new DtoJoinRoom();
        dtoJoinRoom.setRoomId(queue.getRoomId());
        dtoJoinRoom.setName(dto.getName());
        dtoJoinRoom.setEmail(dto.getEmail());

        ProducerRecord<String, DtoJoinRoom> record = new ProducerRecord<>(queue.getKafkaTopic(), dtoJoinRoom);

        logger.info("Sending user {} for room {}", dto.getEmail(), queue.getRoomId());

        newUserProducer.send(record);
    }
}


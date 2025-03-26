package com.example.queue.kafka;

import com.example.queue.dto.DtoLeftQueue;
import com.example.queue.repository.UserInQueueRepository;
import com.example.queue.service.QueueService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class JoinedRoomKafkaListener {

    private final UserInQueueRepository userInQueueRepository;
    private final String joinedRoomTopic = "joined-room-topic";
    private final Logger logger = LoggerFactory.getLogger(JoinedRoomKafkaListener.class);

    @KafkaListener(topics = joinedRoomTopic, groupId = "joined-room-group")
    public void leftQueue(DtoLeftQueue dto)
    {
        userInQueueRepository.deleteByEmail(dto.getEmail());

        logger.info("{} is leaving the queue from room {}", dto.getEmail(), dto.getRoomId());
    }
}

package com.example.room;

import com.example.room.dto.DtoCreateRoom;
import com.example.room.entity.Room;
import com.example.room.repository.RoomRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.util.ArrayList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@AutoConfigureMockMvc
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RoomRepository roomRepository;

    @Test
    public void createRoom_shouldReturnRoomDetails_whenRequestIsValid() throws Exception {

        DtoCreateRoom dto = new DtoCreateRoom();
        dto.setName("Room1");
        dto.setMaxCapacity(30);
        dto.setDuration(10);

        Room room = new Room();
        room.setId("roomId123");
        room.setName(dto.getName());
        room.setMaxCapacity(dto.getMaxCapacity());
        room.setDuration(dto.getDuration());
        room.setPaused(false);
        room.setUsers(new ArrayList<>());
        room.setQueueKafkaTopic("topic");

        Mockito.when(roomRepository.save(any(Room.class))).thenReturn(room);

        // Act & Assert
        mockMvc.perform(MockMvcRequestBuilders.post("/api/room")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new ObjectMapper().writeValueAsString(dto)))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.jsonPath("$.id").value("roomId123"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.name").value("Room1"))
                .andExpect(MockMvcResultMatchers.jsonPath("$.maxCapacity").value(30))
                .andExpect(MockMvcResultMatchers.jsonPath("$.duration").value(10));


        ArgumentCaptor<Room> captor = ArgumentCaptor.forClass(Room.class);
        Mockito.verify(roomRepository, times(2)).save(captor.capture());

        Room capturedDto = captor.getValue();

        assertEquals(capturedDto.getId(), "roomId123");
        assertEquals(capturedDto.getName(), "Room1");
        assertEquals(capturedDto.getMaxCapacity(), 30);
        assertEquals(capturedDto.getDuration(), 10);
    }
}


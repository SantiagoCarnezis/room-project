package com.example.room.controller;

import com.example.room.dto.DtoCreateRoom;
import com.example.room.entity.Room;
import com.example.room.service.RoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;


@RestController
@RequestMapping("/api/room")
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class RoomController {

    private final RoomService roomService;
    private final MessageSource messageSource;

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody DtoCreateRoom dto)
    {
        Room body = roomService.createRoom(dto);
        return new ResponseEntity<>(body, HttpStatus.CREATED);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteRoom(@PathVariable String id)
    {
        roomService.deleteRoom(id);
        String message = messageSource.getMessage("room.deleted", null, Locale.ENGLISH);
        return new ResponseEntity(message, HttpStatus.OK);
    }

    @PutMapping("/pause/{roomId}")
    public ResponseEntity pause(@PathVariable String roomId)
    {
        roomService.pause(roomId);
        String message = messageSource.getMessage("room.pause", new Object[]{roomId}, Locale.ENGLISH);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PutMapping("/resume/{roomId}")
    public ResponseEntity resume(@PathVariable String roomId)
    {
        roomService.resume(roomId);
        String message = messageSource.getMessage("room.resume", new Object[]{roomId}, Locale.ENGLISH);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @PutMapping("/resume-all")
    public ResponseEntity resumeAllRooms()
    {
        roomService.resumeAllRooms();
        String message = messageSource.getMessage("room.resume.all", null, Locale.ENGLISH);
        return new ResponseEntity<>(message, HttpStatus.OK);
    }

    @GetMapping
    public ResponseEntity<List<Room>> getAllRooms() {
        List<Room> rooms = roomService.getAllRooms();
        return new ResponseEntity<>(rooms, HttpStatus.OK);
    }
}

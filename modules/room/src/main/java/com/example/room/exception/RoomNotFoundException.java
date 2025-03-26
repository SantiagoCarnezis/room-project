package com.example.room.exception;

public class RoomNotFoundException extends RuntimeException {

    public RoomNotFoundException(String id) {
        super("room " + id + " not found");
    }
}

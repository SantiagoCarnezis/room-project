package com.example.room.exception;

public class QueueCreationException extends RuntimeException {

    public QueueCreationException(String id) {
        super("could not create queue for room " + id);
    }
}

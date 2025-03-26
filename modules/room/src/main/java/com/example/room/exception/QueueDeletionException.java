package com.example.room.exception;

public class QueueDeletionException extends RuntimeException {

    public QueueDeletionException(String roomId) {
        super("could not delete queue for room " + roomId);
    }
}

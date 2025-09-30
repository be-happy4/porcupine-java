package org.behappy.porcupine.model;

public record ReturnEvent(
        int clientId,
        long ts,
        int id) implements Event {
}

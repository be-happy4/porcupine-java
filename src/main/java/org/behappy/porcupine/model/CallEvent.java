package org.behappy.porcupine.model;

public record CallEvent<T>(
        int clientId,
        T value,
        int id
) implements Event {
}

package org.behappy.porcupine.model;

public record Operation<T>(
        int clientId,
        T input,
        long callTime,
        T output,
        long returnTime) {
}

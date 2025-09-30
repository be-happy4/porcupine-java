package org.behappy.porcupine.model;

public record Pair<X, Y>(X first, Y second) {
    public static <X, Y> Pair<X, Y> of(X first, Y second) {
        return new Pair<>(first, second);
    }
}

package org.behappy.porcupine.visualization;

public record Annotation(
        int clientId,
        String tag,
        long start,
        long end,
        String description,
        String details,
        String textColor,
        String backgroundColor
) {


}

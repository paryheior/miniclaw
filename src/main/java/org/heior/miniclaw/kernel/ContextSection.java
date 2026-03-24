package org.heior.miniclaw.kernel;


public record ContextSection(
        String name,
        String content,
        int priority
) {
}
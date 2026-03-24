package org.heior.miniclaw.kernel;

public enum ContextMode {
    FULL,
    MINIMAL;

    public static ContextMode from(String mode) {
        if(mode == null || mode.isBlank()) {
            return FULL;
        }
        return "minimal".equalsIgnoreCase(mode) ? MINIMAL : FULL;
    }
}

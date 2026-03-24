package org.heior.miniclaw.dto;
import jakarta.validation.constraints.NotBlank;
public record ChatMessageDto(
        @NotBlank String role,
        @NotBlank String content
) {
}
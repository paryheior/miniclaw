package org.heior.miniclaw.dto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ChatRequest(
        String model,
        Double temperature,
        Integer maxTokens,
        Boolean stream,
        String systemPrompt,
        @Valid @NotEmpty List<ChatMessageDto> messages
) {
}
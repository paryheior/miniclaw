package org.heior.miniclaw.controller;
import jakarta.validation.Valid;
import org.heior.miniclaw.agent.AgentContext;
import org.heior.miniclaw.agent.AgentResult;
import org.heior.miniclaw.agent.GeneralAgent;
import org.heior.miniclaw.dto.ChatMessageDto;
import org.heior.miniclaw.dto.ChatRequest;
import org.heior.miniclaw.dto.ChatResponseDto;
import org.heior.miniclaw.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 👈 加上这一行，允许所有域名的前端访问
public class ChatController {

    private final ChatService chatService;
    private final GeneralAgent generalAgent;
    public ChatController(ChatService chatService,
                          GeneralAgent generalAgent) {
        this.chatService = chatService;
        this.generalAgent = generalAgent;
    }


    @PostMapping("/chat")
    public ChatResponseDto chat(@Valid @RequestBody ChatRequest request) {
        String content = chatService.chat(request);
        return new ChatResponseDto(content);
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(@Valid @RequestBody ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        Disposable disposable = chatService.stream(request).subscribe(
                chunk -> sendChunk(emitter, chunk),
                error -> emitter.completeWithError(error),
                () -> {
                    try {
                        emitter.send(SseEmitter.event().data("[DONE]"));
                    } catch (IOException ignored) {
                    }
                    emitter.complete();
                }
        );

        emitter.onCompletion(disposable::dispose);
        emitter.onTimeout(() -> {
            disposable.dispose();
            emitter.complete();
        });

        return emitter;
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().name("message").data(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }


    @PostMapping("/send")
    public AgentResult send(@RequestBody @Valid ChatRequest request) {
        String userMessage = extractLastUserMessage(request.messages());

        AgentContext context = new AgentContext();
        context.setSessionId("default-session");
        context.setUserId("default-user");
        context.setUserMessage(userMessage);
        context.setModel(request.model());
        context.setTemperature(request.temperature());
        context.setMaxTokens(request.maxTokens());
        context.setStream(request.stream());
        context.setSystemPrompt(request.systemPrompt());
        context.setRecentMessages(Collections.emptyList());

        return generalAgent.execute(context);
    }

    private String extractLastUserMessage(List<ChatMessageDto> messages) {
        if (messages == null || messages.isEmpty()) {
            throw new IllegalArgumentException("messages 不能为空");
        }

        return messages.stream()
                .filter(Objects::nonNull)
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .reduce((first, second) -> second)
                .map(ChatMessageDto::content)
                .filter(content -> content != null && !content.isBlank())
                .orElseThrow(() -> new IllegalArgumentException("缺少最后一条 user 消息"));
    }
}
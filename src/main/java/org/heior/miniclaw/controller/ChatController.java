package org.heior.miniclaw.controller;
import jakarta.validation.Valid;
import org.heior.miniclaw.dto.ChatRequest;
import org.heior.miniclaw.dto.ChatResponseDto;
import org.heior.miniclaw.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.Disposable;

import java.io.IOException;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*") // 👈 加上这一行，允许所有域名的前端访问
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
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
}
package org.heior.miniclaw.controller;

import jakarta.validation.Valid;
import org.heior.miniclaw.agent.AgentContext;
import org.heior.miniclaw.agent.AgentResult;
import org.heior.miniclaw.agent.GeneralAgent;
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
@CrossOrigin(origins = "*")
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
    public SseEmitter chatStream(@Valid @RequestBody ChatRequest request) {
        return emitChatStream(request);
    }

    @PostMapping("/send")
    public AgentResult send(@RequestBody @Valid ChatRequest request) {
        return generalAgent.execute(buildAgentContext(request));
    }

    /**
     * 前端当前请求的是 /api/send/stream，因此这里必须提供对应接口。
     * 目前 AgentResult 是一次性返回结构；真正的 Agent 流式执行后续可以再抽象。
     * 这里先用 ChatService 的流式输出保证前端 SSE 可以正常工作。
     */
    @PostMapping(value = "/send/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter sendStream(@RequestBody @Valid ChatRequest request) {
        return emitChatStream(request);
    }

    private SseEmitter emitChatStream(ChatRequest request) {
        SseEmitter emitter = new SseEmitter(0L);

        Disposable disposable = chatService.stream(request).subscribe(
                chunk -> sendChunk(emitter, chunk),
                error -> sendError(emitter, error),
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

    private AgentContext buildAgentContext(ChatRequest request) {
        AgentContext context = new AgentContext();
        context.setSessionId("default-session");
        context.setUserId("default-user");
        context.setUserMessage(request.lastUserMessage());
        context.setModel(request.model());
        context.setTemperature(request.temperature());
        context.setMaxTokens(request.maxTokens());
        context.setStream(request.stream());
        context.setSystemPrompt(request.systemPrompt());
        return context;
    }

    private void sendChunk(SseEmitter emitter, String chunk) {
        try {
            emitter.send(SseEmitter.event().name("message").data(chunk));
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    private void sendError(SseEmitter emitter, Throwable error) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data(error.getMessage() == null ? "模型调用失败" : error.getMessage()));
        } catch (IOException ignored) {
        }
        emitter.completeWithError(error);
    }
}

package org.heior.miniclaw.service;

import org.heior.miniclaw.dto.ChatMessageDto;
import org.heior.miniclaw.dto.ChatRequest;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;

@Service
public class ChatService {

    private final ChatClient chatClient;

    // 🌟 修复：直接注入我们在 Config 中配置好并绑定了 Tools 的 ChatClient
    // 使用 @Qualifier 指定 Bean 名称，避免与其他可能的 ChatClient 冲突
    public ChatService(@Qualifier("miniClawChatClient") ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    public String chat(ChatRequest request) {
        Prompt prompt = buildPrompt(request);
        return chatClient.prompt(prompt).call().content();
    }

    public Flux<String> stream(ChatRequest request) {
        Prompt prompt = buildPrompt(request);
        return chatClient.prompt(prompt).stream().content();
    }

    private Prompt buildPrompt(ChatRequest request) {
        List<Message> promptMessages = new ArrayList<>();

        if (hasText(request.systemPrompt())) {
            promptMessages.add(new SystemMessage(request.systemPrompt()));
        }

        for (ChatMessageDto msg : request.messages()) {
            if (!hasText(msg.content())) {
                continue;
            }

            String role = msg.role() == null ? "user" : msg.role().trim().toLowerCase();

            switch (role) {
                case "system" -> promptMessages.add(new SystemMessage(msg.content()));
                case "assistant" -> promptMessages.add(new AssistantMessage(msg.content()));
                case "user" -> promptMessages.add(new UserMessage(msg.content()));
                default -> promptMessages.add(new UserMessage(msg.content()));
            }
        }

        ChatOptions.Builder optionsBuilder = ChatOptions.builder();

        if (hasText(request.model())) {
            optionsBuilder.model(request.model());
        }
        if (request.temperature() != null) {
            optionsBuilder.temperature(request.temperature());
        }
        if (request.maxTokens() != null) {
            optionsBuilder.maxTokens(request.maxTokens());
        }

        return new Prompt(promptMessages, optionsBuilder.build());
    }

    private boolean hasText(String text) {
        return text != null && !text.isBlank();
    }
}
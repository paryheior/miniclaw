package org.heior.miniclaw.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LocalModelConfig {

    @Bean("localExecChatClient")
    public ChatClient localExecChatClient(OllamaChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }
}

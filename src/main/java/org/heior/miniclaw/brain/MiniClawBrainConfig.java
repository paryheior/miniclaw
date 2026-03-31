package org.heior.miniclaw.brain;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.Arrays;
import java.util.List;

@Configuration
public class MiniClawBrainConfig {

    @Bean
    public DashScopeApi dashScopeApi() {
        return DashScopeApi.builder()
                .apiKey(System.getenv("AI_DASHSCOPE_API_KEY"))
                .build();
    }

    @Bean
    public ChatModel miniClawChatModel(DashScopeApi dashScopeApi) {
        // 你可以先用 qwen-plus，后面再换
        DashScopeChatOptions options = DashScopeChatOptions.builder()
                .model("qwen-plus")
                .temperature(0.3)
                .build();

        return DashScopeChatModel.builder()
                .dashScopeApi(dashScopeApi)
                .defaultOptions(options)
                .build();
    }

    @Bean
    public ChatClient miniClawChatClient(ChatModel miniClawChatModel,
                                         ObjectProvider<ToolCallbackProvider> providers) {

        List<ToolCallback> callbacks = providers.orderedStream()
                .flatMap(provider -> Arrays.stream(provider.getToolCallbacks()))
                .toList();

        return ChatClient.builder(miniClawChatModel)
                .defaultToolCallbacks(callbacks.toArray(new ToolCallback[0]))
                .build();
    }
}
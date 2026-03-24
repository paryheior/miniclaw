package org.heior.miniclaw.config;


import org.heior.miniclaw.tool.MiniClawTools;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.ai.tool.StaticToolCallbackProvider;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider miniClawToolCallbackProvider(MiniClawTools miniClawTools) {
        return new StaticToolCallbackProvider(ToolCallbacks.from(miniClawTools));
    }
}

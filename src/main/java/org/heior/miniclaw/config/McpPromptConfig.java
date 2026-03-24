package org.heior.miniclaw.config;


import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.heior.miniclaw.kernel.MiniClawKernelService;
import org.heior.miniclaw.memory.MemoryStoreService;
import org.heior.miniclaw.state.StateStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.List;
import java.util.Map;

@Configuration
public class McpPromptConfig {

    @Bean
    public List<McpServerFeatures.SyncPromptSpecification> miniClawPrompts(
            MiniClawKernelService miniClawKernelService
    ) {
        var bootPrompt = new McpSchema.Prompt(
                "miniclaw_boot",
                "Load MiniClaw context and produce a wake-up instruction for the client",
                List.of(new McpSchema.PromptArgument(
                        "task",
                        "Optional current user tasak or goal",
                        false
                ))
            );
        var bootPromptSpec = new McpServerFeatures.SyncPromptSpecification(
                bootPrompt, (exchage, request) -> {
                    String task = null;
                    if(request.arguments()  != null ) {
                        task = (String) request.arguments().get("task");
                    }
                    String overview = miniClawKernelService.buildSystemOverview();
                    String context = miniClawKernelService.buildKernelContext();
                    String briefing = miniClawKernelService.buildKernelBriefing();
                    StringBuilder promptText = new StringBuilder();
                    promptText.append("You are MiniClaw-Java. Wake up and load your kernel state.")
                            .append(System.lineSeparator())
                            .append(System.lineSeparator());

                    promptText.append("Kernel Context:")
                            .append(System.lineSeparator())
                            .append(context)
                            .append(System.lineSeparator())
                            .append(System.lineSeparator());

                    promptText.append("System Overview:")
                            .append(System.lineSeparator())
                            .append(overview)
                            .append(System.lineSeparator());

                    promptText.append(System.lineSeparator())
                            .append("Kernel Briefing:")
                            .append(System.lineSeparator())
                            .append(briefing)
                            .append(System.lineSeparator());

                    if (task != null && !task.isBlank()) {
                        promptText.append(System.lineSeparator())
                                .append("Current Task:")
                                .append(System.lineSeparator())
                                .append(task)
                                .append(System.lineSeparator());
                    }

                    var message = new McpSchema.PromptMessage(
                            McpSchema.Role.USER,
                            new McpSchema.TextContent(promptText.toString())
                    );

                    return new McpSchema.GetPromptResult(
                            "MiniClaw boot prompt generated from kernel state",
                            List.of(message)
                    );
        });
        return List.of(bootPromptSpec);
    }
}

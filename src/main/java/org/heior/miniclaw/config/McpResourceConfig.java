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
public class McpResourceConfig {
    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> miniClawResources(
            MiniClawKernelService miniClawKernelService
    )  {
        var contextResource = McpSchema.Resource.builder()
                .uri("miniclaw://context")
                .name("MiniClaw Global Context")
                .title("MiniClaw Global Context")
                .description("Assembled MiniClaw context")
                .mimeType("text/markdown")
                .build();
        var statusResource = McpSchema.Resource.builder()
                .uri("miniclaw://status")
                .name("MiniClaw Status")
                .title("MiniClaw Status")
                .description("MiniClaw runtime status")
                .mimeType("application/json")
                .build();

        var overviewResource = McpSchema.Resource.builder()
                .uri("miniclaw://overview")
                .name("MiniClaw Overview")
                .title("MiniClaw Overview")
                .description("MiniClaw kernel overview")
                .mimeType("text/plain")
                .build();

        var contextSpec = new McpServerFeatures.SyncResourceSpecification(
                contextResource, (exchange, request) -> new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(
                                request.uri(),
                                "text/markdown",
                                miniClawKernelService.buildKernelContext()
                        ))
                )
        );

        var statusSpec = new McpServerFeatures.SyncResourceSpecification(
            statusResource, (exchange, request) -> {
                try {
                    return new McpSchema.ReadResourceResult(
                            List.of(new McpSchema.TextResourceContents(
                                    request.uri(),
                                    "application/json",
                                    miniClawKernelService.buildKernelStatus()
                            ))
                    );
                } catch (Exception e) {
                    throw new RuntimeException("Failed to build miniclaw://status", e);
                }
            }
        );

        var overviewSpec = new McpServerFeatures.SyncResourceSpecification(
                overviewResource, (exchange, request) -> new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(
                                request.uri(),
                                "text/plain",
                                miniClawKernelService.buildKernelContext()
                        ))
                )
        );

        var briefingResource = McpSchema.Resource.builder()
                .uri("miniclaw://briefing")
                .name("miniclaw-briefing")
                .title("MiniClaw Briefing")
                .description("MiniClaw kernel briefing")
                .mimeType("text/plain")
                .build();

        var briefingSpec = new McpServerFeatures.SyncResourceSpecification(
                briefingResource, (exchange, request) -> new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(
                                request.uri(),
                                "text/plain",
                                miniClawKernelService.buildKernelBriefing()
                        ))
                )
        );

        var introspectResource = McpSchema.Resource.builder()
                .uri("miniclaw://introspect")
                .name("miniclaw-introspection")
                .title("MiniClaw Introspection")
                .description("MiniClaw introspection")
                .mimeType("text/plain")
                .build();

        var introspectSpec = new McpServerFeatures.SyncResourceSpecification(
                introspectResource, (exchange, request) -> new McpSchema.ReadResourceResult(
                        List.of(new McpSchema.TextResourceContents(
                                request.uri(),
                                "text/plain",
                                miniClawKernelService.buildKernelIntrospection()
                        ))
                )
        );
        return List.of(contextSpec, statusSpec, overviewSpec, briefingSpec, introspectSpec);
    }
}

package org.heior.miniclaw.debug;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@RestController
public class ToolRegistryDebugController {

    private final ObjectProvider<ToolCallbackProvider> toolCallbackProviders;

    public ToolRegistryDebugController(ObjectProvider<ToolCallbackProvider> toolCallbackProviders) {
        this.toolCallbackProviders = toolCallbackProviders;
    }

    @GetMapping("/debug/miniclaw/tools")
    public List<String> tools() {
        Set<String> names = new LinkedHashSet<>();

        toolCallbackProviders.orderedStream().forEach(provider -> {
            ToolCallback[] callbacks = provider.getToolCallbacks();
            if (callbacks != null) {
                for (ToolCallback callback : callbacks) {
                    names.add(callback.getToolDefinition().name());
                }
            }
        });

        return new ArrayList<>(names);
    }
}
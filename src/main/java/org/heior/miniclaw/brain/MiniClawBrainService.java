package org.heior.miniclaw.brain;


import org.heior.miniclaw.kernel.ContextMode;
import org.heior.miniclaw.kernel.MiniClawKernelService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class MiniClawBrainService {

    private final ChatClient chatClient;
    private final MiniClawKernelService miniClawKernelService;

    public MiniClawBrainService(ChatClient miniClawChatClient,
                                MiniClawKernelService miniClawKernelService) {
        this.chatClient = miniClawChatClient;
        this.miniClawKernelService = miniClawKernelService;
    }

    public String chat(String userInput, String mode) {
        ContextMode contextMode = ContextMode.from(mode);

        String briefing = miniClawKernelService.buildKernelBriefing();
        String context = miniClawKernelService.buildKernelContext(contextMode);
        String overview = miniClawKernelService.buildSystemOverview();

        return this.chatClient.prompt()
                .system("""
                        You are MiniClaw-Java, an agent brain attached to a local kernel.
                        Always use the provided kernel briefing and kernel context.
                        When useful, call tools instead of guessing.
                        Prefer concise, grounded answers.
                        If the user asks about state, memory, skills, entities, or files, use tools.
                        """)
                .user("""
                        Kernel Briefing:
                        %s

                        Kernel Context:
                        %s

                        System Overview:
                        %s

                        User Request:
                        %s
                        """.formatted(briefing, context, overview, userInput))
                .call()
                .content();
    }
}
package org.heior.miniclaw.agent.impl;


import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import org.heior.miniclaw.agent.AgentContext;
import org.heior.miniclaw.agent.AgentResult;
import org.heior.miniclaw.agent.BaseAgent;
import org.heior.miniclaw.agent.router.KnowledgeModelType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class KnowledgeAgent implements BaseAgent {

    private final KnowledgeModelRouter knowledgeModelRouter;
    private final ChatClient aliKnowledgeChatClient;

    public KnowledgeAgent(KnowledgeModelRouter knowledgeModelRouter,
                          @Qualifier("aliKnowledgeChatClient") ChatClient aliKnowledgeChatClient) {
        this.knowledgeModelRouter = knowledgeModelRouter;
        this.aliKnowledgeChatClient = aliKnowledgeChatClient;
    }

    @Override
    public String name() {
        return "KnowledgeAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        if (context == null || !StringUtils.hasText(context.getUserMessage())) {
            return AgentResult.fail(name(), null, "用户输入不能为空");
        }

        try {
            KnowledgeModelType modelType = knowledgeModelRouter.route(context);
            DashScopeChatOptions options = buildOptions(modelType);
            String modelName = resolveModelName(modelType);

            String content = aliKnowledgeChatClient.prompt()
                    .system(buildSystemPrompt(context))
                    .user(context.getUserMessage())
                    .options(options)
                    .call()
                    .content();

            return AgentResult.success(name(), modelName, content);
        } catch (Exception e) {
            return AgentResult.fail(name(), null, "KnowledgeAgent 执行失败: " + e.getMessage());
        }
    }

    private DashScopeChatOptions buildOptions(KnowledgeModelType modelType) {
        return switch (modelType) {
            case FAST -> DashScopeChatOptions.builder()
                    .model("qwen-plus")
                    .temperature(0.3)
                    .build();
            case REASONING -> DashScopeChatOptions.builder()
                    .model("qwen-max")
                    .temperature(0.2)
                    .build();
            case LONG_CONTEXT -> DashScopeChatOptions.builder()
                    .model("qwen-long")
                    .temperature(0.2)
                    .build();
        };
    }

    private String resolveModelName(KnowledgeModelType modelType) {
        return switch (modelType) {
            case FAST -> "qwen-plus";
            case REASONING -> "qwen-max";
            case LONG_CONTEXT -> "qwen-long";
        };
    }

    private String buildSystemPrompt(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 MiniClaw 的知识型 Agent。");
        sb.append("你的职责是给出准确、清晰、有条理的知识性回答。");
        sb.append("如果问题涉及概念解释、方案分析、代码理解或文档问答，应优先提供结构化回答。");

        if (StringUtils.hasText(context.getSystemPrompt())) {
            sb.append(" 用户补充的系统要求如下：");
            sb.append(context.getSystemPrompt());
        }

        return sb.toString();
    }
}
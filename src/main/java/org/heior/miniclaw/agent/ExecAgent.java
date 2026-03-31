package org.heior.miniclaw.agent;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ExecAgent implements BaseAgent{

    public final ChatClient localExecChatClient;


    public ExecAgent(@Qualifier("localExecChatClient") ChatClient localExecChatClient) {
        this.localExecChatClient = localExecChatClient;
    }

    @Override
    public String name() {
        return ExecAgent.class.getName();
    }

    @Override
    public AgentResult execute(AgentContext context) {
        if (context == null || !StringUtils.hasText(context.getUserMessage())) {
            return AgentResult.fail(name(), null, "用户输入不能为空");
        }

        try {
            String systemPrompt = buildSystemPrompt(context);
            String userPrompt = context.getUserMessage();

            String content = localExecChatClient.prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            return AgentResult.success(name(), "local-exec-model", content);
        } catch (Exception e) {
            return AgentResult.fail(name(), "local-exec-model", "ExecAgent 执行失败: " + e.getMessage());
        }
    }
    private String buildSystemPrompt(AgentContext context) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 MiniClaw 的执行型 Agent。");
        sb.append("你的职责是理解用户的执行类请求，给出清晰、可操作、尽量可靠的结果。");
        sb.append("如果用户要求执行命令，你先理解意图，再输出简洁的执行建议、步骤或结果分析。");
        sb.append("回答要直接、准确，不要输出无关内容。");

        if (StringUtils.hasText(context.getSystemPrompt())) {
            sb.append(" 用户补充的系统要求如下：");
            sb.append(context.getSystemPrompt());
        }

        return sb.toString();
    }
}

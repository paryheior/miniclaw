package org.heior.miniclaw.agent;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.heior.miniclaw.agent.impl.KnowledgeAgent;
@Component
public class GeneralAgent implements BaseAgent {

    private final ExecAgent execAgent;
    private final KnowledgeAgent knowledgeAgent;

    public GeneralAgent(ExecAgent execAgent, KnowledgeAgent knowledgeAgent) {
        this.execAgent = execAgent;
        this.knowledgeAgent = knowledgeAgent;
    }

    @Override
    public String name() {
        return "GeneralAgent";
    }

    @Override
    public AgentResult execute(AgentContext context) {
        if (context == null || !StringUtils.hasText(context.getUserMessage())) {
            return AgentResult.fail(name(), null, "用户输入不能为空");
        }

        try {
            AgentTarget target = route(context.getUserMessage());

            return switch (target) {
                case EXEC -> execAgent.execute(context);
                case KNOWLEDGE -> knowledgeAgent.execute(context);
            };
        } catch (Exception e) {
            return AgentResult.fail(name(), null, "GeneralAgent 路由失败: " + e.getMessage());
        }
    }

    /**
     * 第一版先用规则路由
     */
    private AgentTarget route(String userMessage) {
        String text = userMessage.toLowerCase();

        if (isExecTask(text)) {
            return AgentTarget.EXEC;
        }

        return AgentTarget.KNOWLEDGE;
    }

    /**
     * 是否属于执行类任务
     */
    private boolean isExecTask(String text) {
        return text.contains("执行")
                || text.contains("运行")
                || text.contains("命令")
                || text.contains("shell")
                || text.contains("terminal")
                || text.contains("bash")
                || text.contains("cmd")
                || text.contains("powershell")
                || text.contains("git ")
                || text.contains("mvn ")
                || text.contains("npm ")
                || text.contains("pnpm ")
                || text.contains("python ")
                || text.contains("java ");
    }

    private enum AgentTarget {
        EXEC,
        KNOWLEDGE
    }
}
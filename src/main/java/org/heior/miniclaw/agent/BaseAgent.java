package org.heior.miniclaw.agent;

public interface BaseAgent {
    String name();
    AgentResult execute(AgentContext context);
}

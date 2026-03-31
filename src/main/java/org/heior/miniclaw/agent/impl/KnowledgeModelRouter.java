package org.heior.miniclaw.agent.impl;

import org.heior.miniclaw.agent.AgentContext;
import org.heior.miniclaw.agent.router.KnowledgeModelType;

public interface KnowledgeModelRouter {
    KnowledgeModelType route(AgentContext context);
}

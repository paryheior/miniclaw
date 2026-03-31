package org.heior.miniclaw.agent.impl;

import org.heior.miniclaw.agent.AgentContext;
import org.heior.miniclaw.agent.router.KnowledgeModelType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;



@Component
public class DefaultKnowledgeModelRouter implements KnowledgeModelRouter{

    @Override
    public KnowledgeModelType route(AgentContext context) {
        if (context == null || !StringUtils.hasText(context.getUserMessage())) {
            return KnowledgeModelType.FAST;
        }

        String text = context.getUserMessage().trim();
        String lowerText = text.toLowerCase();

        if (isLongContextTask(text, lowerText)) {
            return KnowledgeModelType.LONG_CONTEXT;
        }

        if (isReasoningTask(lowerText)) {
            return KnowledgeModelType.REASONING;
        }

        return KnowledgeModelType.FAST;
    }

    private boolean isLongContextTask(String text, String lowerText) {
        return text.length() > 2000
                || lowerText.contains("长文")
                || lowerText.contains("全文")
                || lowerText.contains("完整代码")
                || lowerText.contains("整篇")
                || lowerText.contains("完整分析")
                || lowerText.contains("逐段")
                || lowerText.contains("逐句")
                || lowerText.contains("详细解读");
    }

    private boolean isReasoningTask(String lowerText) {
        return lowerText.contains("为什么")
                || lowerText.contains("原因")
                || lowerText.contains("分析")
                || lowerText.contains("推理")
                || lowerText.contains("比较")
                || lowerText.contains("区别")
                || lowerText.contains("优缺点")
                || lowerText.contains("方案")
                || lowerText.contains("设计")
                || lowerText.contains("如何实现")
                || lowerText.contains("怎么做")
                || lowerText.contains("证明")
                || lowerText.contains("是否合理");
    }
}

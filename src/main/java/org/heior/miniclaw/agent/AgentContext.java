package org.heior.miniclaw.agent;


import java.util.ArrayList;
import java.util.List;

public class AgentContext {

    /**
     * 会话ID
     */
    private String sessionId;

    /**
     * 用户ID
     */
    private String userId;

    /**
     * 当前用户输入（最后一条 user message）
     */
    private String userMessage;

    /**
     * 前端传入的模型名（可选）
     * 例如：qwen-plus / qwen-max / local-llm
     */
    private String model;

    /**
     * 温度参数
     */
    private Double temperature;

    /**
     * 最大输出 token
     */
    private Integer maxTokens;

    /**
     * 是否流式返回
     */
    private Boolean stream;

    /**
     * 自定义系统提示词
     */
    private String systemPrompt;

    /**
     * 最近几轮消息摘要/历史消息
     * 第一版先简单存字符串，后面可替换成 Message 对象
     */
    private List<String> recentMessages = new ArrayList<>();

    public AgentContext() {
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }

    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }

    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }

    public Boolean getStream() {
        return stream;
    }

    public void setStream(Boolean stream) {
        this.stream = stream;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public List<String> getRecentMessages() {
        return recentMessages;
    }

    public void setRecentMessages(List<String> recentMessages) {
        this.recentMessages = recentMessages;
    }
}
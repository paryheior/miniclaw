package org.heior.miniclaw.agent;

public class AgentResult {

    /**
     * 处理本次请求的 Agent 名称
     * 例如：GeneralAgent / ExecAgent / KnowledgeAgent
     */
    private String agentName;

    /**
     * 实际使用的模型名
     * 例如：local-qwen / qwen-plus / qwen-max
     */
    private String modelName;

    /**
     * 是否执行成功
     */
    private boolean success;

    /**
     * 返回内容
     */
    private String output;

    /**
     * 错误信息（失败时可用）
     */
    private String errorMessage;

    public AgentResult() {
    }

    public AgentResult(String agentName, String modelName, boolean success, String output, String errorMessage) {
        this.agentName = agentName;
        this.modelName = modelName;
        this.success = success;
        this.output = output;
        this.errorMessage = errorMessage;
    }

    public static AgentResult success(String agentName, String modelName, String output) {
        return new AgentResult(agentName, modelName, true, output, null);
    }

    public static AgentResult fail(String agentName, String modelName, String errorMessage) {
        return new AgentResult(agentName, modelName, false, null, errorMessage);
    }

    public String getAgentName() {
        return agentName;
    }

    public void setAgentName(String agentName) {
        this.agentName = agentName;
    }

    public String getModelName() {
        return modelName;
    }

    public void setModelName(String modelName) {
        this.modelName = modelName;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getOutput() {
        return output;
    }

    public void setOutput(String output) {
        this.output = output;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
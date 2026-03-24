package org.heior.miniclaw.analytics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniClawAnalytics {

    private Map<String, Integer> toolCalls = new HashMap<>();
    private Map<String, Integer> fileChanges = new HashMap<>();
    private List<Integer> activeHours = new ArrayList<>();
    private String lastActivity;


    public MiniClawAnalytics() {
        for (int i = 0; i < 24; i++) {
            activeHours.add(0);
        }
    }

    public Map<String, Integer> getToolCalls() {
        return toolCalls;
    }

    public void setToolCalls(Map<String, Integer> toolCalls) {
        this.toolCalls = toolCalls;
    }

    public Map<String, Integer> getFileChanges() {
        return fileChanges;
    }

    public void setFileChanges(Map<String, Integer> fileChanges) {
        this.fileChanges = fileChanges;
    }

    public List<Integer> getActiveHours() {
        return activeHours;
    }

    public void setActiveHours(List<Integer> activeHours) {
        this.activeHours = activeHours;
    }

    public String getLastActivity() {
        return lastActivity;
    }

    public void setLastActivity(String lastActivity) {
        this.lastActivity = lastActivity;
    }
}

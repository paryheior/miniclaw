package org.heior.miniclaw.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Entity {
    private String name;
    private String type; // person / project / tool / concept / place / other
    private Map<String, String> attributes = new HashMap<>();
    private List<String> relations = new ArrayList<>();
    private String firstMentioned;
    private String lastMentioned;
    private int mentionCount;
    private String sentiment;

    public void setName(String name) {
        this.name = name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
    }

    public void setRelations(List<String> relations) {
        this.relations = relations;
    }

    public void setFirstMentioned(String firstMentioned) {
        this.firstMentioned = firstMentioned;
    }

    public void setLastMentioned(String lastMentioned) {
        this.lastMentioned = lastMentioned;
    }

    public void setMentionCount(int mentionCount) {
        this.mentionCount = mentionCount;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public List<String> getRelations() {
        return relations;
    }

    public String getFirstMentioned() {
        return firstMentioned;
    }

    public String getLastMentioned() {
        return lastMentioned;
    }

    public int getMentionCount() {
        return mentionCount;
    }

    public String getSentiment() {
        return sentiment;
    }
}

package org.heior.miniclaw.observer;

public class ObserverPattern {
    private String type;
    private double confidence;
    private String description;
    private String suggestion;

    public ObserverPattern() {
    }

    public ObserverPattern(String type, double confidence, String description, String suggestion) {
        this.type = type;
        this.confidence = confidence;
        this.description = description;
        this.suggestion = suggestion;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}

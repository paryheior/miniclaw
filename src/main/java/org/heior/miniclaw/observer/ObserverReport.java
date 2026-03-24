package org.heior.miniclaw.observer;


import java.util.ArrayList;
import java.util.List;

public class ObserverReport {

    private String timestamp;
    private List<ObserverPattern> patterns = new ArrayList<>();


    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public List<ObserverPattern> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<ObserverPattern> patterns) {
        this.patterns = patterns;
    }


}

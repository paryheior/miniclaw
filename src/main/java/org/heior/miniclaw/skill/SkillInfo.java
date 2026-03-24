package org.heior.miniclaw.skill;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

public class SkillInfo {

    private String name;
    private String description;
    private String toolName;
    private String toolDescription;
    private List<String> files = new ArrayList<>();
    private String fileToolName;
    private String fileToolDescription;

    public String getFileToolName() {
        return fileToolName;
    }

    public void setFileToolName(String fileToolName) {
        this.fileToolName = fileToolName;
    }

    public String getFileToolDescription() {
        return fileToolDescription;
    }

    public void setFileToolDescription(String fileToolDescription) {
        this.fileToolDescription = fileToolDescription;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String toolName) {
        this.toolName = toolName;
    }

    public String getToolDescription() {
        return toolDescription;
    }

    public void setToolDescription(String toolDescription) {
        this.toolDescription = toolDescription;
    }

    public List<String> getFiles() {
        return files;
    }

    public void setFiles(List<String> files) {
        this.files = files;
    }
}

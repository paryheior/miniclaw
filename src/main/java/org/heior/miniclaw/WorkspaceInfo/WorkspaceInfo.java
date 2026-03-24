package org.heior.miniclaw.WorkspaceInfo;

import java.util.ArrayList;
import java.util.List;

public class WorkspaceInfo {
    private String path;
    private String name;
    private boolean gitRepo;
    private String gitBranch;
    private String gitStatus;
    private List<String> techStack = new ArrayList<>();

    public void setPath(String path) {
        this.path = path;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setGitRepo(boolean gitRepo) {
        this.gitRepo = gitRepo;
    }

    public void setGitBranch(String gitBranch) {
        this.gitBranch = gitBranch;
    }

    public void setGitStatus(String gitStatus) {
        this.gitStatus = gitStatus;
    }

    public void setTechStack(List<String> techStack) {
        this.techStack = techStack;
    }

    public String getPath() {
        return path;
    }

    public String getName() {
        return name;
    }

    public boolean isGitRepo() {
        return gitRepo;
    }

    public String getGitBranch() {
        return gitBranch;
    }

    public String getGitStatus() {
        return gitStatus;
    }

    public List<String> getTechStack() {
        return techStack;
    }
}

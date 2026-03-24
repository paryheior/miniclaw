package org.heior.miniclaw.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;
import java.nio.file.Paths;

@ConfigurationProperties(prefix = "miniclaw")
public class MiniClawProperties {
    private String homeDir = Paths.get(System.getProperty("user.home"), ".miniclaw-java").toString();
    boolean bootstrapEnabled = true;

    public Path getHomePath() {
        return Paths.get(homeDir);
    }

    public String getHomeDir() {
        return homeDir;
    }

    public void setHomeDir(String homeDir) {
        this.homeDir = homeDir;
    }

    public boolean isBootstrapEnabled() {
        return bootstrapEnabled;
    }

    public void setBootstrapEnabled(boolean bootstrapEnabled) {
        this.bootstrapEnabled = bootstrapEnabled;
    }
}

package org.heior.miniclaw.analytics;

import com.fasterxml.jackson.core.exc.StreamReadException;
import com.fasterxml.jackson.databind.DatabindException;
import org.heior.miniclaw.config.MiniClawProperties;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AnalyticsStoreService {

    private final MiniClawProperties properties;
    private final ObjectMapper objectMapper;

    public AnalyticsStoreService(MiniClawProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Path getAnalyticsPath() {
        return properties.getHomePath().resolve("analytics.json");
    }

    public MiniClawAnalytics load() {
        Path path = getAnalyticsPath();
        try {
            if (Files.notExists(path)) {
                MiniClawAnalytics analytics = new MiniClawAnalytics();
                save(analytics);
                return analytics;
            }
            return objectMapper.readValue(path.toFile(), MiniClawAnalytics.class);
        } catch (IOException e) {
            throw new RuntimeException("读取 state.json 失败: " + e.getMessage(), e);
        }
    }

    public void save(MiniClawAnalytics analytics) {
        try {
            Files.createDirectories(properties.getHomePath());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(getAnalyticsPath().toFile(), analytics);
        } catch (IOException e) {
            throw new RuntimeException("写入 analytics.json 失败: " + e.getMessage(), e);
        }
    }

    public void trackTool(String toolName) {
        if (toolName == null || toolName.isBlank()) {
            return;
        }

        MiniClawAnalytics analytics = load();
        analytics.getToolCalls().put(toolName,
                analytics.getToolCalls().getOrDefault(toolName, 0) + 1);

        int hour = LocalDateTime.now().getHour();
        analytics.getActiveHours().set(hour, analytics.getActiveHours().get(hour) + 1);
        analytics.setLastActivity(OffsetDateTime.now().toString());

        save(analytics);
    }

    public void trackFileChange(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }

        MiniClawAnalytics analytics = load();
        analytics.getFileChanges().put(fileName,
                analytics.getFileChanges().getOrDefault(fileName, 0) + 1);

        int hour = LocalDateTime.now().getHour();
        analytics.getActiveHours().set(hour, analytics.getActiveHours().get(hour) + 1);
        analytics.setLastActivity(OffsetDateTime.now().toString());

        save(analytics);
    }

    public String renderSummary() {
        MiniClawAnalytics analytics = load();

        String topTools = analytics.getToolCalls().isEmpty()
                ? "(none)"
                : analytics.getToolCalls().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));

        String topFiles = analytics.getFileChanges().isEmpty()
                ? "(none)"
                : analytics.getFileChanges().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));

        int busiestHour = 0;
        int maxCount = -1;
        for (int i = 0; i < analytics.getActiveHours().size(); i++) {
            if (analytics.getActiveHours().get(i) > maxCount) {
                maxCount = analytics.getActiveHours().get(i);
                busiestHour = i;
            }
        }

        return """
                MiniClaw Analytics
                - lastActivity: %s
                - topTools: %s
                - topFiles: %s
                - busiestHour: %02d:00
                - activeHours: %s
                """.formatted(
                analytics.getLastActivity(),
                topTools,
                topFiles,
                busiestHour,
                analytics.getActiveHours()
        );
    }

    public String getTopToolsText(int limit) {
        MiniClawAnalytics analytics = load();
        if (analytics.getToolCalls() == null || analytics.getToolCalls().isEmpty()) {
            return "(none)";
        }

        return analytics.getToolCalls().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    public String getTopFilesText(int limit) {
        MiniClawAnalytics analytics = load();
        if (analytics.getFileChanges() == null || analytics.getFileChanges().isEmpty()) {
            return "(none)";
        }

        return analytics.getFileChanges().entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(limit)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(", "));
    }

    public String getBusiestHourText() {
        MiniClawAnalytics analytics = load();
        if (analytics.getActiveHours() == null || analytics.getActiveHours().isEmpty()) {
            return "(unknown)";
        }

        int busiestHour = 0;
        int maxCount = -1;
        for (int i = 0; i < analytics.getActiveHours().size(); i++) {
            if (analytics.getActiveHours().get(i) > maxCount) {
                maxCount = analytics.getActiveHours().get(i);
                busiestHour = i;
            }
        }

        return String.format("%02d:00", busiestHour);
    }
}
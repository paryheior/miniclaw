package org.heior.miniclaw.observer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.heior.miniclaw.config.MiniClawProperties;
import org.heior.miniclaw.memory.MemoryStoreService;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ObserverService {

    private final MiniClawProperties properties;
    private final MemoryStoreService memoryStoreService;
    private final ObjectMapper objectMapper;

    public ObserverService(MiniClawProperties properties,
                           MemoryStoreService memoryStoreService,
                           ObjectMapper objectMapper) {
        this.properties = properties;
        this.memoryStoreService = memoryStoreService;
        this.objectMapper = objectMapper;
    }

    public Path getObserverReportPath() {
        return properties.getHomePath().resolve("observer-patterns.json");
    }

    public ObserverReport analyzeRecentLogs() {
        var entries = memoryStoreService.readTodayLogEntries();
        boolean usedFallback = false;

        if (entries.isEmpty()) {
            entries = memoryStoreService.readLatestNonEmptyLogEntries();
            usedFallback = !entries.isEmpty();
        }

        ObserverReport report = new ObserverReport();
        report.setTimestamp(OffsetDateTime.now().toString());

        if (entries.isEmpty()) {
            saveReport(report);
            return report;
        }

        List<ObserverPattern> patterns = new ArrayList<>();

        addToolUsagePattern(entries, patterns);
        addErrorPattern(entries, patterns);
        addQuestionPattern(entries, patterns);
        addFeedbackPattern(entries, patterns);
        addVolumePattern(entries, usedFallback, patterns);

        report.setPatterns(patterns);
        saveReport(report);
        return report;
    }

    public ObserverReport loadLastReport() {
        Path path = getObserverReportPath();
        try {
            if (Files.notExists(path)) {
                return new ObserverReport();
            }
            return objectMapper.readValue(path.toFile(), ObserverReport.class);
        } catch (IOException e) {
            throw new RuntimeException("读取 observer-patterns.json 失败: " + e.getMessage(), e);

        }

    }

    public String renderReport(ObserverReport report) {
        StringBuilder sb = new StringBuilder();
        sb.append("MiniClaw Observer Report").append(System.lineSeparator());
        sb.append("- timestamp: ").append(report.getTimestamp()).append(System.lineSeparator());

        if (report.getPatterns() == null || report.getPatterns().isEmpty()) {
            sb.append("- patterns: none").append(System.lineSeparator());
            return sb.toString();
        }

        sb.append(System.lineSeparator()).append("Patterns").append(System.lineSeparator());
        for (ObserverPattern pattern : report.getPatterns()) {
            sb.append("- [").append(pattern.getType()).append("] ")
                    .append(pattern.getDescription())
                    .append(" | confidence=").append(pattern.getConfidence());

            if (pattern.getSuggestion() != null && !pattern.getSuggestion().isBlank()) {
                sb.append(" | suggestion=").append(pattern.getSuggestion());
            }
            sb.append(System.lineSeparator());
        }

        return sb.toString();
    }

    public boolean shouldSuggestDistill(ObserverReport report, long dailyLogBytes, int entryCount) {
        if (dailyLogBytes > 8_000) {
            return true;
        }
        if (entryCount > 10) {
            return true;
        }
        if (report == null || report.getPatterns() == null) {
            return false;
        }

        for (ObserverPattern pattern : report.getPatterns()) {
            if ("volume".equals(pattern.getType()) && pattern.getConfidence() >= 0.7) {
                return true;
            }
            if ("error".equals(pattern.getType()) && pattern.getConfidence() >= 0.7) {
                return true;
            }
        }

        return false;
    }

    private void addToolUsagePattern(List<String> entries, List<ObserverPattern> patterns) {
        Pattern toolPattern = Pattern.compile("\\b(miniclaw_[a-zA-Z0-9_]+|skill_[a-zA-Z0-9_]+)\\b");
        Map<String, Integer> counts = new HashMap<>();

        for (String entry : entries) {
            Matcher matcher = toolPattern.matcher(entry);
            while (matcher.find()) {
                String tool = matcher.group(1);
                counts.put(tool, counts.getOrDefault(tool, 0) + 1);
            }
        }

        if (!counts.isEmpty()) {
            var top = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .orElse(null);

            if (top != null && top.getValue() >= 2) {
                patterns.add(new ObserverPattern(
                        "tool_usage",
                        Math.min(0.95, top.getValue() / 5.0),
                        "高频工具使用: " + top.getKey() + " x" + top.getValue(),
                        "可以考虑把这个工作流沉淀成更稳定的 skill 或 briefing。"
                ));
            }
        }
    }

    private void addErrorPattern(List<String> entries, List<ObserverPattern> patterns) {
        String[] keywords = {"error", "failed", "exception", "wrong", "报错", "失败", "错误"};
        int hits = countKeywordHits(entries, keywords);

        if (hits > 0) {
            patterns.add(new ObserverPattern(
                    "error",
                    Math.min(0.95, hits / 5.0),
                    "检测到错误相关日志 " + hits + " 次",
                    "建议在 growup 时总结问题与修复方案。"
            ));
        }
    }

    private void addQuestionPattern(List<String> entries, List<ObserverPattern> patterns) {
        int hits = 0;
        for (String entry : entries) {
            if (entry.contains("?") || entry.contains("？") || entry.contains("怎么") || entry.contains("如何")) {
                hits++;
            }
        }

        if (hits > 0) {
            patterns.add(new ObserverPattern(
                    "question",
                    Math.min(0.9, hits / 5.0),
                    "检测到提问/求助型日志 " + hits + " 次",
                    "可以把高频问题整理进 skill 或 reflection。"
            ));
        }
    }

    private void addFeedbackPattern(List<String> entries, List<ObserverPattern> patterns) {
        int positive = countKeywordHits(entries, new String[]{"很好", "不错", "成功", "great", "nice", "thanks", "谢谢"});
        int negative = countKeywordHits(entries, new String[]{"不好", "不对", "失败", "bad", "wrong", "error", "报错"});

        if (positive > 0 || negative > 0) {
            String sentiment = positive >= negative ? "positive" : "negative";
            int total = positive + negative;

            patterns.add(new ObserverPattern(
                    "feedback",
                    Math.min(0.9, total / 5.0),
                    "检测到反馈模式: " + sentiment + " (positive=" + positive + ", negative=" + negative + ")",
                    "可以根据用户反馈调整技能、提示词或长期记忆。"
            ));
        }
    }

    private void addVolumePattern(List<String> entries, boolean usedFallback, List<ObserverPattern> patterns) {
        int size = entries.size();
        if (size == 0) {
            return;
        }

        if (size <= 3) {
            patterns.add(new ObserverPattern(
                    "volume",
                    0.4,
                    "近期日志较少，共 " + size + " 条" + (usedFallback ? "（使用最近非空日志）" : ""),
                    "先继续积累上下文，再做蒸馏。"
            ));
        } else if (size <= 10) {
            patterns.add(new ObserverPattern(
                    "volume",
                    0.6,
                    "近期日志适中，共 " + size + " 条" + (usedFallback ? "（使用最近非空日志）" : ""),
                    "当前节奏正常，可按需 growup。"
            ));
        } else {
            patterns.add(new ObserverPattern(
                    "volume",
                    0.85,
                    "近期日志较多，共 " + size + " 条" + (usedFallback ? "（使用最近非空日志）" : ""),
                    "建议执行 growup/distill，整理长期记忆。"
            ));
        }
    }

    private int countKeywordHits(List<String> entries, String[] keywords) {
        int hits = 0;
        for (String entry : entries) {
            String lower = entry.toLowerCase();
            for (String keyword : keywords) {
                if (lower.contains(keyword.toLowerCase())) {
                    hits++;
                    break;
                }
            }
        }
        return hits;
    }

    private void saveReport(ObserverReport report) {
        try {
            Files.createDirectories(properties.getHomePath());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(getObserverReportPath().toFile(), report);
        } catch (IOException e) {
            throw new RuntimeException("写入 observer-patterns.json 失败: " + e.getMessage(), e);
        }
    }
}
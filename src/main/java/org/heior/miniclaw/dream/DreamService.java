package org.heior.miniclaw.dream;


import org.heior.miniclaw.analytics.AnalyticsStoreService;
import org.heior.miniclaw.config.MiniClawProperties;
import org.heior.miniclaw.entity.EntityStoreService;
import org.heior.miniclaw.observer.ObserverReport;
import org.heior.miniclaw.observer.ObserverService;
import org.heior.miniclaw.skill.SkillStoreService;
import org.heior.miniclaw.state.StateStoreService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class DreamService {

    private final MiniClawProperties properties;
    private final ObserverService observerService;
    private final AnalyticsStoreService analyticsStoreService;
    private final StateStoreService stateStoreService;
    private final SkillStoreService skillStoreService;
    private final EntityStoreService entityStoreService;

    public DreamService(MiniClawProperties properties,
                        ObserverService observerService,
                        AnalyticsStoreService analyticsStoreService,
                        StateStoreService stateStoreService,
                        SkillStoreService skillStoreService,
                        EntityStoreService entityStoreService) {
        this.properties = properties;
        this.observerService = observerService;
        this.analyticsStoreService = analyticsStoreService;
        this.stateStoreService = stateStoreService;
        this.skillStoreService = skillStoreService;
        this.entityStoreService = entityStoreService;
    }

    public Path getDreamLogPath() {
        return properties.getHomePath().resolve("DREAM.md");
    }

    public String buildDreamReport() {
        var observerReport = observerService.analyzeRecentLogs();
        var runtimeState = stateStoreService.load();

        int skillCount = skillStoreService.listSkills().size();
        int entityCount = entityStoreService.list(null).size();

        String topTools = analyticsStoreService.getTopToolsText(3);
        String topFiles = analyticsStoreService.getTopFilesText(3);

        List<String> themes = deriveThemes(observerReport, runtimeState.isNeedsDistill(), skillCount, entityCount, topTools);
        List<String> meanings = deriveMeanings(observerReport, runtimeState.isNeedsDistill(), skillCount, entityCount);
        List<String> nextMoves = deriveNextMoves(observerReport, runtimeState.isNeedsDistill(), skillCount, entityCount, topFiles);

        StringBuilder sb = new StringBuilder();
        sb.append("MiniClaw Dream Report").append(System.lineSeparator());
        sb.append("- date: ").append(LocalDate.now()).append(System.lineSeparator());
        sb.append("- topTools: ").append(topTools).append(System.lineSeparator());
        sb.append("- topFiles: ").append(topFiles).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("Themes").append(System.lineSeparator());
        for (String theme : themes) {
            sb.append("- ").append(theme).append(System.lineSeparator());
        }

        sb.append(System.lineSeparator());
        sb.append("Meaning").append(System.lineSeparator());
        for (String meaning : meanings) {
            sb.append("- ").append(meaning).append(System.lineSeparator());
        }

        sb.append(System.lineSeparator());
        sb.append("Next Moves").append(System.lineSeparator());
        for (String next : nextMoves) {
            sb.append("- ").append(next).append(System.lineSeparator());
        }

        return sb.toString();
    }

    public String performDream() {
        String report = buildDreamReport();
        appendDreamLog(report);
        return "Dream 完成。"
                + System.lineSeparator()
                + System.lineSeparator()
                + report;
    }

    private List<String> deriveThemes(ObserverReport report,
                                      boolean needsDistill,
                                      int skillCount,
                                      int entityCount,
                                      String topTools) {
        List<String> themes = new ArrayList<>();

        if (report.getPatterns() == null || report.getPatterns().isEmpty()) {
            themes.add("当前还没有稳定的行为模式。");
        } else {
            boolean hasTool = report.getPatterns().stream().anyMatch(p -> "tool_usage".equalsIgnoreCase(p.getType()));
            boolean hasQuestion = report.getPatterns().stream().anyMatch(p -> "question".equalsIgnoreCase(p.getType()));
            boolean hasError = report.getPatterns().stream().anyMatch(p -> "error".equalsIgnoreCase(p.getType()));
            boolean hasVolume = report.getPatterns().stream().anyMatch(p -> "volume".equalsIgnoreCase(p.getType()));

            if (hasTool) themes.add("系统正在形成固定工具使用习惯。");
            if (hasQuestion) themes.add("系统仍处在高频学习/求解阶段。");
            if (hasError) themes.add("系统近期存在调试与修复主题。");
            if (hasVolume) themes.add("日志体量已经能反映工作节奏。");
        }

        if (needsDistill) {
            themes.add("短期记忆压力正在上升。");
        }

        if (skillCount <= 1) {
            themes.add("技能系统仍处于萌芽阶段。");
        } else if (skillCount <= 5) {
            themes.add("技能系统正在形成初步骨架。");
        } else {
            themes.add("技能系统正在走向多器官化。");
        }

        if (entityCount <= 2) {
            themes.add("实体图谱还很稀疏。");
        } else {
            themes.add("实体图谱开始具备结构性。");
        }

        if (!"(none)".equals(topTools)) {
            themes.add("当前最明显的行为重心集中在: " + topTools);
        }

        return themes;
    }

    private List<String> deriveMeanings(ObserverReport report,
                                        boolean needsDistill,
                                        int skillCount,
                                        int entityCount) {
        List<String> meanings = new ArrayList<>();

        boolean hasError = report.getPatterns() != null
                && report.getPatterns().stream().anyMatch(p -> "error".equalsIgnoreCase(p.getType()));
        boolean hasQuestion = report.getPatterns() != null
                && report.getPatterns().stream().anyMatch(p -> "question".equalsIgnoreCase(p.getType()));
        boolean hasTool = report.getPatterns() != null
                && report.getPatterns().stream().anyMatch(p -> "tool_usage".equalsIgnoreCase(p.getType()));

        if (hasQuestion && skillCount < 3) {
            meanings.add("这说明系统还在不断遇到新问题，但技能沉淀速度还不够快。");
        }

        if (hasTool) {
            meanings.add("这说明部分工作流已经开始重复，适合固化为稳定 skill。");
        }

        if (hasError) {
            meanings.add("这说明系统当前主要处于构建与调试阶段，而不是纯执行阶段。");
        }

        if (needsDistill) {
            meanings.add("这说明短期记忆开始堆积，系统需要整理经验以维持清晰上下文。");
        } else {
            meanings.add("这说明当前记忆负荷仍在可控范围内。");
        }

        if (entityCount < 3) {
            meanings.add("这说明系统对外部世界的结构化记忆仍较薄弱。");
        } else {
            meanings.add("这说明系统已经开始通过实体图谱形成稳定对象认知。");
        }

        if (meanings.isEmpty()) {
            meanings.add("当前系统处在平稳积累阶段，尚未出现强烈演化信号。");
        }

        return meanings;
    }

    private List<String> deriveNextMoves(ObserverReport report,
                                         boolean needsDistill,
                                         int skillCount,
                                         int entityCount,
                                         String topFiles) {
        List<String> nextMoves = new ArrayList<>();

        if (needsDistill) {
            nextMoves.add("优先执行 growup / archive，降低短期记忆压力。");
        }

        boolean hasQuestion = report.getPatterns() != null
                && report.getPatterns().stream().anyMatch(p -> "question".equalsIgnoreCase(p.getType()));
        boolean hasTool = report.getPatterns() != null
                && report.getPatterns().stream().anyMatch(p -> "tool_usage".equalsIgnoreCase(p.getType()));
        boolean hasError = report.getPatterns() != null
                && report.getPatterns().stream().anyMatch(p -> "error".equalsIgnoreCase(p.getType()));

        if (hasQuestion) {
            nextMoves.add("把高频问题整理成新的 skill 或 skill 附属文档。");
        }

        if (hasTool) {
            nextMoves.add("把高频工具调用链整理成 workflow 说明。");
        }

        if (hasError) {
            nextMoves.add("把调试经验写入 REFLECTION.md 或 skill references。");
        }

        if (skillCount < 3) {
            nextMoves.add("继续扩展技能系统，优先覆盖当前高频任务。");
        }

        if (entityCount < 3) {
            nextMoves.add("继续补关键实体及其关系，增强长期结构化记忆。");
        }

        if (!"(none)".equals(topFiles)) {
            nextMoves.add("重点关注最近高频改动文件: " + topFiles);
        }

        if (nextMoves.isEmpty()) {
            nextMoves.add("保持当前节奏，继续积累行为数据。");
        }

        return nextMoves;
    }

    private void appendDreamLog(String text) {
        Path file = getDreamLogPath();
        try {
            if (Files.notExists(file)) {
                Files.writeString(file, "# Dream" + System.lineSeparator(), StandardCharsets.UTF_8);
            }

            Files.writeString(
                    file,
                    text + System.lineSeparator() + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("写入 DREAM.md 失败: " + e.getMessage(), e);
        }
    }
}
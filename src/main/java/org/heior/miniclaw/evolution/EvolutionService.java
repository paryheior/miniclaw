package org.heior.miniclaw.evolution;


import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class EvolutionService {

    private final MiniClawProperties properties;
    private final ObjectMapper objectMapper;
    private final ObserverService observerService;
    private final AnalyticsStoreService analyticsStoreService;
    private final StateStoreService stateStoreService;
    private final SkillStoreService skillStoreService;
    private final EntityStoreService entityStoreService;

    public EvolutionService(MiniClawProperties properties,
                            ObjectMapper objectMapper,
                            ObserverService observerService,
                            AnalyticsStoreService analyticsStoreService,
                            StateStoreService stateStoreService,
                            SkillStoreService skillStoreService,
                            EntityStoreService entityStoreService) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.observerService = observerService;
        this.analyticsStoreService = analyticsStoreService;
        this.stateStoreService = stateStoreService;
        this.skillStoreService = skillStoreService;
        this.entityStoreService = entityStoreService;
    }

    public Path getEvolutionStatePath() {
        return properties.getHomePath().resolve("evolution-state.json");
    }

    public Path getEvolutionLogPath() {
        return properties.getHomePath().resolve("EVOLUTION.md");
    }

    public EvolutionState loadState() {
        Path path = getEvolutionStatePath();
        try {
            if (Files.notExists(path)) {
                EvolutionState state = new EvolutionState();
                state.setLastEvolution(null);
                state.setTotalEvolutions(0);
                saveState(state);
                return state;
            }
            return objectMapper.readValue(path.toFile(), EvolutionState.class);
        } catch (IOException e) {
            throw new RuntimeException("读取 evolution-state.json 失败: " + e.getMessage(), e);

        }

    }

    public void saveState(EvolutionState state) {
        try {
            Files.createDirectories(properties.getHomePath());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(getEvolutionStatePath().toFile(), state);
        } catch (IOException e) {
            throw new RuntimeException("写入 evolution-state.json 失败: " + e.getMessage(), e);
        }
    }

    public String buildEvolutionReport() {
        var state = loadState();
        var observerReport = observerService.analyzeRecentLogs();
        String analyticsSummary = analyticsStoreService.renderSummary();

        int skillCount = skillStoreService.listSkills().size();
        int entityCount = entityStoreService.list(null).size();

        StringBuilder sb = new StringBuilder();
        sb.append("MiniClaw Evolution Report").append(System.lineSeparator());
        sb.append("- lastEvolution: ").append(state.getLastEvolution()).append(System.lineSeparator());
        sb.append("- totalEvolutions: ").append(state.getTotalEvolutions()).append(System.lineSeparator());
        sb.append("- skillCount: ").append(skillCount).append(System.lineSeparator());
        sb.append("- entityCount: ").append(entityCount).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append(observerService.renderReport(observerReport)).append(System.lineSeparator());
        sb.append(analyticsSummary).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("Evolution Signals").append(System.lineSeparator());
        for (String signal : deriveSignals(observerReport, skillCount, entityCount)) {
            sb.append("- ").append(signal).append(System.lineSeparator());
        }

        return sb.toString();
    }

    public String performEvolution() {
        var observerReport = observerService.analyzeRecentLogs();
        var state = loadState();
        var runtimeState = stateStoreService.load();
        int skillCount = skillStoreService.listSkills().size();
        int entityCount = entityStoreService.list(null).size();

        List<String> signals = deriveSignals(observerReport, skillCount, entityCount);
        List<String> mutations = deriveMutations(observerReport, runtimeState.isNeedsDistill(), skillCount, entityCount);

        state.setTotalEvolutions(state.getTotalEvolutions() + 1);
        state.setLastEvolution(OffsetDateTime.now().toString());
        saveState(state);

        StringBuilder block = new StringBuilder();
        block.append("## Evolution ").append(LocalDate.now()).append(System.lineSeparator());
        block.append("- generation: ").append(state.getTotalEvolutions()).append(System.lineSeparator());
        block.append("- lastEvolution: ").append(state.getLastEvolution()).append(System.lineSeparator());
        block.append("- skillCount: ").append(skillCount).append(System.lineSeparator());
        block.append("- entityCount: ").append(entityCount).append(System.lineSeparator());
        block.append("- needsDistill: ").append(runtimeState.isNeedsDistill()).append(System.lineSeparator());

        block.append("- signals:").append(System.lineSeparator());
        for (String signal : signals) {
            block.append("  - ").append(signal).append(System.lineSeparator());
        }

        block.append("- mutations:").append(System.lineSeparator());
        for (String mutation : mutations) {
            block.append("  - ").append(mutation).append(System.lineSeparator());
        }

        appendEvolutionLog(block.toString());

        return "Evolution 完成。"
                + System.lineSeparator()
                + System.lineSeparator()
                + block;
    }

    private List<String> deriveSignals(ObserverReport report,
                                       int skillCount,
                                       int entityCount) {
        List<String> signals = new ArrayList<>();

        if (report.getPatterns() == null || report.getPatterns().isEmpty()) {
            signals.add("当前没有显著行为模式。");
        } else {
            report.getPatterns().stream().limit(5)
                    .forEach(pattern -> signals.add(
                            pattern.getType() + ": " + pattern.getDescription()
                    ));
        }

        if (skillCount == 0) {
            signals.add("当前没有技能，系统扩展能力较弱。");
        } else if (skillCount <= 3) {
            signals.add("技能数量仍较少，适合继续沉淀高频流程。");
        } else {
            signals.add("技能系统已初步成型。");
        }

        if (entityCount == 0) {
            signals.add("实体图谱为空。");
        } else if (entityCount <= 5) {
            signals.add("实体图谱开始形成。");
        } else {
            signals.add("实体图谱正在扩展。");
        }

        return signals;
    }

    private List<String> deriveMutations(ObserverReport report,
                                         boolean needsDistill,
                                         int skillCount,
                                         int entityCount) {
        List<String> mutations = new ArrayList<>();

        boolean hasErrorPattern = report.getPatterns() != null && report.getPatterns().stream()
                .anyMatch(pattern -> "error".equalsIgnoreCase(pattern.getType()));

        boolean hasQuestionPattern = report.getPatterns() != null && report.getPatterns().stream()
                .anyMatch(pattern -> "question".equalsIgnoreCase(pattern.getType()));

        boolean hasToolPattern = report.getPatterns() != null && report.getPatterns().stream()
                .anyMatch(pattern -> "tool_usage".equalsIgnoreCase(pattern.getType()));

        if (needsDistill) {
            mutations.add("建议强化 growup/distill 流程，降低短期记忆压力。");
        }

        if (hasErrorPattern) {
            mutations.add("建议把近期修复经验写入 REFLECTION.md 或 skill 文档。");
        }

        if (hasQuestionPattern) {
            mutations.add("建议把高频问题沉淀为新的 skill。");
        }

        if (hasToolPattern) {
            mutations.add("建议把高频工具组合整理成 workflow skill。");
        }

        if (skillCount < 2) {
            mutations.add("建议继续扩展技能系统，优先覆盖高频任务。");
        }

        if (entityCount < 3) {
            mutations.add("建议持续补全关键实体与关系。");
        }

        if (mutations.isEmpty()) {
            mutations.add("当前系统结构稳定，保持现有演化节奏。");
        }

        return mutations;
    }

    private void appendEvolutionLog(String text) {
        Path file = getEvolutionLogPath();
        try {
            if (Files.notExists(file)) {
                Files.writeString(file, "# Evolution" + System.lineSeparator(), StandardCharsets.UTF_8);
            }

            Files.writeString(
                    file,
                    text + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("写入 EVOLUTION.md 失败: " + e.getMessage(), e);
        }
    }
}
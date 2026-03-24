package org.heior.miniclaw.kernel;

import org.heior.miniclaw.WorkspaceInfo.WorkspaceService;
import org.heior.miniclaw.analytics.AnalyticsStoreService;
import org.heior.miniclaw.archive.ArchiveService;
import org.heior.miniclaw.dream.DreamService;
import org.heior.miniclaw.entity.EntityStoreService;
import org.heior.miniclaw.evolution.EvolutionService;
import org.heior.miniclaw.memory.MemoryStoreService;
import org.heior.miniclaw.observer.ObserverService;
import org.heior.miniclaw.skill.SkillStoreService;
import org.heior.miniclaw.state.StateStoreService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;


@Service
public class MiniClawKernelService {
    private final Logger logger = LoggerFactory.getLogger(MiniClawKernelService.class.getName());
    private final MemoryStoreService memoryStoreService;
    private final StateStoreService stateStoreService;
    private final WorkspaceService workspaceService;
    private final SkillStoreService skillStoreService;
    private final EntityStoreService entityStoreService;
    private final ObserverService observerService;
    private final AnalyticsStoreService analyticsStoreService;
    private final ContextAssembler contextAssembler;
    private final ArchiveService archiveService;
    private final EvolutionService evolutionService;
    private final DreamService dreamService;

    public MiniClawKernelService(MemoryStoreService memoryStoreService,
                                 StateStoreService stateStoreService,
                                 WorkspaceService workspaceService,
                                 SkillStoreService skillStoreService,
                                 EntityStoreService entityStoreService,
                                 ObserverService observerService,
                                 AnalyticsStoreService analyticsStoreService,
                                 ContextAssembler contextAssembler,
                                 ArchiveService archiveService,
                                 EvolutionService evolutionService,
                                 DreamService dreamService) {
        this.entityStoreService = entityStoreService;
        this.skillStoreService = skillStoreService;
        this.stateStoreService = stateStoreService;
        this.workspaceService = workspaceService;
        this.memoryStoreService = memoryStoreService;
        this.observerService = observerService;
        this.analyticsStoreService = analyticsStoreService;
        this.contextAssembler = contextAssembler;
        this.archiveService = archiveService;
        this.evolutionService = evolutionService;
        this.dreamService = dreamService;
    }


    public String buildDreamReport() {
        return dreamService.buildDreamReport();
    }

    public String performDream() {
        return dreamService.performDream();
    }

    public String archiveTodayLog() {
        return archiveService.archiveTodayLog();
    }

    public String archiveLatestLog() {
        return archiveService.archiveLatestNonEmptyLog();
    }

    public String buildArchiveSummary() {
        var files = archiveService.listArchivedLogs();
        StringBuilder sb = new StringBuilder();
        sb.append("MiniClaw Archive Summary").append(System.lineSeparator());

        if (files.isEmpty()) {
            sb.append("- archivedLogs: none").append(System.lineSeparator());
            return sb.toString();
        }

        sb.append("- archivedCount: ").append(files.size()).append(System.lineSeparator());
        sb.append("Recent Archived Logs").append(System.lineSeparator());

        for (String file : files.stream().limit(10).toList()) {
            sb.append("- ").append(file).append(System.lineSeparator());
        }

        return sb.toString();
    }
    public String buildAnalyticsSummary() {
        return analyticsStoreService.renderSummary();
    }
    /**
     * 统一上下文入口
     */
    public String buildKernelContext() {
        return buildKernelContext(ContextMode.FULL);
    }

    public String buildKernelContext(ContextMode mode) {
        var workspace = workspaceService.detect();
        var skills = skillStoreService.listSkills();
        var entities = entityStoreService.list(null);

        return contextAssembler.assemble(
                mode,
                workspace,
                buildKernelBriefing(),
                skills,
                entities,
                memoryStoreService.readFile("IDENTITY.md"),
                memoryStoreService.readFile("SOUL.md"),
                memoryStoreService.readFile("USER.md"),
                memoryStoreService.readFile("AGENTS.md"),
                memoryStoreService.readFile("MEMORY.md"),
                memoryStoreService.readDailyLog()
        );
    }

    /**
     * 统一状态入口
     */
    public String buildKernelStatus() {
        var state = stateStoreService.load();
        var workspace = workspaceService.detect();
        var skills = skillStoreService.listSkills();
        var entities = entityStoreService.list(null);

        return """
                MiniClaw Kernel Status
                - lastHeartbeat: %s
                - lastDistill: %s
                - needsDistill: %s
                - dailyLogBytes: %d

                Workspace
                - path: %s
                - name: %s
                - gitRepo: %s
                - gitBranch: %s
                - gitStatus: %s
                - techStack: %s

                Summary
                - skillCount: %d
                - entityCount: %d
                """.formatted(
                state.getLastHeartbeat(),
                state.getLastDistill(),
                state.isNeedsDistill(),
                state.getDailyLogBytes(),

                workspace.getPath(),
                workspace.getName(),
                workspace.isGitRepo(),
                workspace.getGitBranch(),
                workspace.getGitStatus(),
                workspace.getTechStack(),

                skills.size(),
                entities.size()
        );
    }

    /**
     * skills 摘要
     */
    public String buildSkillSummary() {
        var skills = skillStoreService.listSkills();
        if (skills.isEmpty()) {
            return "No skills";
        }

        StringBuilder sb = new StringBuilder("Skills Summary:\n");
        for (var skill : skills) {
            sb.append("- ").append(skill.getName())
                    .append(" : ").append(skill.getDescription())
                    .append(" | tool=").append(skill.getToolName())
                    .append(" | fileTool=").append(skill.getFileToolName())
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * entities 摘要
     */
    public String buildEntitySummary() {
        var entities = entityStoreService.list(null);
        if (entities.isEmpty()) {
            return "No entities";
        }

        StringBuilder sb = new StringBuilder("Entity Summary:\n");
        for (var entity : entities) {
            sb.append("- ").append(entity.getName())
                    .append(" (").append(entity.getType()).append(")")
                    .append(" mentions=").append(entity.getMentionCount())
                    .append(" relations=").append(entity.getRelations())
                    .append("\n");
        }
        return sb.toString();
    }

    /**
     * 一个总览接口，给调试或后续 prompt/resource 用
     */
    public String buildSystemOverview() {
        return buildKernelStatus()
                + "\n\n"
                + buildSkillSummary()
                + "\n\n"
                + buildEntitySummary();
    }


    public String buildKernelBriefing() {
        var state = stateStoreService.load();
        var workspace = workspaceService.detect();
        var skills = skillStoreService.listSkills();
        var entities = entityStoreService.list(null);
        var report = observerService.analyzeRecentLogs();

        String topTools = analyticsStoreService.getTopToolsText(3);
        String topFiles = analyticsStoreService.getTopFilesText(3);
        String busiestHour = analyticsStoreService.getBusiestHourText();

        StringBuilder sb = new StringBuilder();
        sb.append("MiniClaw Kernel Briefing").append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("Status").append(System.lineSeparator());
        sb.append("- workspace: ").append(workspace.getName()).append(System.lineSeparator());
        sb.append("- path: ").append(workspace.getPath()).append(System.lineSeparator());
        sb.append("- git: ").append(workspace.isGitRepo()
                ? workspace.getGitBranch() + " / " + workspace.getGitStatus()
                : "(not a git repo)").append(System.lineSeparator());
        sb.append("- techStack: ").append(workspace.getTechStack()).append(System.lineSeparator());
        sb.append("- lastHeartbeat: ").append(state.getLastHeartbeat()).append(System.lineSeparator());
        sb.append("- lastDistill: ").append(state.getLastDistill()).append(System.lineSeparator());
        sb.append("- needsDistill: ").append(state.isNeedsDistill()).append(System.lineSeparator());
        sb.append("- dailyLogBytes: ").append(state.getDailyLogBytes()).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("Analytics").append(System.lineSeparator());
        sb.append("- topTools: ").append(topTools).append(System.lineSeparator());
        sb.append("- topFiles: ").append(topFiles).append(System.lineSeparator());
        sb.append("- busiestHour: ").append(busiestHour).append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append("Skills").append(System.lineSeparator());
        if (skills.isEmpty()) {
            sb.append("- (no skills)").append(System.lineSeparator());
        } else {
            for (var skill : skills.stream().limit(3).toList()) {
                sb.append("- ").append(skill.getName())
                        .append(" : ").append(skill.getDescription())
                        .append(" | tool=").append(skill.getToolName())
                        .append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());

        sb.append("Entities").append(System.lineSeparator());
        if (entities.isEmpty()) {
            sb.append("- (no entities)").append(System.lineSeparator());
        } else {
            for (var entity : entities.stream().limit(3).toList()) {
                sb.append("- ").append(entity.getName())
                        .append(" (").append(entity.getType()).append(")")
                        .append(" mentions=").append(entity.getMentionCount())
                        .append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());

        sb.append("Observed Patterns").append(System.lineSeparator());
        if (report.getPatterns() == null || report.getPatterns().isEmpty()) {
            sb.append("- (no patterns)").append(System.lineSeparator());
        } else {
            for (var pattern : report.getPatterns().stream().limit(3).toList()) {
                sb.append("- [").append(pattern.getType()).append("] ")
                        .append(pattern.getDescription())
                        .append(System.lineSeparator());
            }
        }
        sb.append(System.lineSeparator());

        sb.append("Advice").append(System.lineSeparator());
        if (state.isNeedsDistill()) {
            sb.append("- Memory pressure detected, recommend growup/distill now.").append(System.lineSeparator());
        } else {
            sb.append("- System healthy, continue current work.").append(System.lineSeparator());
        }

        if (!workspace.isGitRepo()) {
            sb.append("- Current workspace is not a git repository.").append(System.lineSeparator());
        }

        if ("(none)".equals(topTools)) {
            sb.append("- No strong tool usage pattern yet.").append(System.lineSeparator());
        } else {
            sb.append("- Keep leveraging your top tools: ").append(topTools).append(System.lineSeparator());
        }

        if (report.getPatterns() != null) {
            boolean hasErrorPattern = report.getPatterns().stream()
                    .anyMatch(pattern -> "error".equalsIgnoreCase(pattern.getType()));
            if (hasErrorPattern) {
                sb.append("- Error-related patterns detected, consider summarizing fixes in REFLECTION.md.")
                        .append(System.lineSeparator());
            }
        }

        return sb.toString();
    }
    public String buildEvolutionReport() {
        return evolutionService.buildEvolutionReport();
    }

    public String performEvolution() {
        return evolutionService.performEvolution();
    }
    public String buildKernelIntrospection() {
        var state = stateStoreService.load();
        var workspace = workspaceService.detect();
        var skills = skillStoreService.listSkills();
        var entities = entityStoreService.list(null);
        int todayEntries = memoryStoreService.countTodayLogEntries();

        StringBuilder sb = new StringBuilder();
        sb.append("MiniClaw Introspection").append(System.lineSeparator());
        sb.append("- workspace: ").append(workspace.getName()).append(System.lineSeparator());
        sb.append("- skillCount: ").append(skills.size()).append(System.lineSeparator());
        sb.append("- entityCount: ").append(entities.size()).append(System.lineSeparator());
        sb.append("- todayLogEntries: ").append(todayEntries).append(System.lineSeparator());
        sb.append("- dailyLogBytes: ").append(state.getDailyLogBytes()).append(System.lineSeparator());
        sb.append("- lastHeartbeat: ").append(state.getLastHeartbeat()).append(System.lineSeparator());
        sb.append("- lastDistill: ").append(state.getLastDistill()).append(System.lineSeparator());
        sb.append("- needsDistill: ").append(state.isNeedsDistill()).append(System.lineSeparator());
        sb.append(System.lineSeparator());

        sb.append("Observations").append(System.lineSeparator());

        if (todayEntries == 0) {
            sb.append("- 今日还没有日志记录。").append(System.lineSeparator());
        } else if (todayEntries <= 3) {
            sb.append("- 今日日志较少，系统活动较轻。").append(System.lineSeparator());
        } else if (todayEntries <= 10) {
            sb.append("- 今日日志量适中，工作节奏正常。").append(System.lineSeparator());
        } else {
            sb.append("- 今日日志较多，建议做一次 growup/distill。").append(System.lineSeparator());
        }

        if (state.isNeedsDistill()) {
            sb.append("- 当前状态提示需要蒸馏长期记忆。").append(System.lineSeparator());
        }

        if (!workspace.isGitRepo()) {
            sb.append("- 当前工作区不是 Git 仓库。").append(System.lineSeparator());
        }

        if (skills.isEmpty()) {
            sb.append("- 当前没有可用 skill。").append(System.lineSeparator());
        } else {
            sb.append("- 当前已有 ").append(skills.size()).append(" 个 skill 可用。").append(System.lineSeparator());
        }

        if (entities.isEmpty()) {
            sb.append("- 当前实体图谱为空。").append(System.lineSeparator());
        } else {
            sb.append("- 当前实体图谱已有 ").append(entities.size()).append(" 个实体。").append(System.lineSeparator());
        }

        return sb.toString();
    }

    public String performGrowup() {
        var entries = memoryStoreService.readTodayLogEntries();
        boolean usedFallback = false;

        if (entries.isEmpty()) {
            entries = memoryStoreService.readLatestNonEmptyLogEntries();
            usedFallback = !entries.isEmpty();
        }

        if (entries.isEmpty()) {
            return "今日没有日志可蒸馏，且未找到最近的非空日志。";
        }

        int total = entries.size();
        java.util.List<String> recent = entries.subList(Math.max(0, total - 5), total);

        StringBuilder summary = new StringBuilder();
        summary.append("## Growup ").append(java.time.LocalDate.now()).append(System.lineSeparator());
        summary.append("- source: ").append(usedFallback ? "latest-non-empty-log" : "today-log").append(System.lineSeparator());
        summary.append("- totalEntries: ").append(total).append(System.lineSeparator());
        summary.append("- recentFocus:").append(System.lineSeparator());
        for (String entry : recent) {
            summary.append("  - ").append(stripTimestamp(entry)).append(System.lineSeparator());
        }

        summary.append("- reflection: ");
        if (total <= 3) {
            summary.append("近期活动较少，建议继续积累上下文。").append(System.lineSeparator());
        } else if (total <= 10) {
            summary.append("近期活动结构清晰，当前工作节奏正常。").append(System.lineSeparator());
        } else {
            summary.append("近期活动较密集，建议把高价值经验整理进长期记忆。").append(System.lineSeparator());
        }

        memoryStoreService.appendReflection(summary.toString());
        stateStoreService.markDistilled();

        return "Growup 完成。" + System.lineSeparator() + System.lineSeparator() + summary;
    }

    public String buildObserverReport() {
        var report = observerService.analyzeRecentLogs();
        return observerService.renderReport(report);
    }

    public String buildDistillSuggestion() {
        var report = observerService.analyzeRecentLogs();
        var state = stateStoreService.load();
        int entryCount = memoryStoreService.readTodayLogEntries().size();

        if (entryCount == 0) {
            entryCount = memoryStoreService.readLatestNonEmptyLogEntries().size();
        }

        boolean suggest = observerService.shouldSuggestDistill(report, state.getDailyLogBytes(), entryCount);

        StringBuilder sb = new StringBuilder();
        sb.append("MiniClaw Distill Suggestion").append(System.lineSeparator());
        sb.append("- suggestDistill: ").append(suggest).append(System.lineSeparator());
        sb.append("- dailyLogBytes: ").append(state.getDailyLogBytes()).append(System.lineSeparator());
        sb.append("- logEntryCount: ").append(entryCount).append(System.lineSeparator());
        sb.append(System.lineSeparator());
        sb.append(observerService.renderReport(report));

        if (suggest) {
            sb.append(System.lineSeparator())
                    .append("Conclusion").append(System.lineSeparator())
                    .append("- 建议执行 growup/distill。").append(System.lineSeparator());
        } else {
            sb.append(System.lineSeparator())
                    .append("Conclusion").append(System.lineSeparator())
                    .append("- 当前暂不需要 distill。").append(System.lineSeparator());
        }

        return sb.toString();
    }
    private String stripTimestamp(String entry) {
        return entry.replaceFirst("^- \\[[^\\]]+\\]\\s*", "");
    }
}

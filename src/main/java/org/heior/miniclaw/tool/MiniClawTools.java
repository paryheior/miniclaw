package org.heior.miniclaw.tool;


import org.heior.miniclaw.analytics.AnalyticsStoreService;
import org.heior.miniclaw.entity.Entity;
import org.heior.miniclaw.entity.EntityStoreService;
import org.heior.miniclaw.kernel.ContextMode;
import org.heior.miniclaw.kernel.MiniClawKernelService;
import org.heior.miniclaw.memory.MemoryStoreService;
import org.heior.miniclaw.skill.SkillStoreService;
import org.heior.miniclaw.state.StateStoreService;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MiniClawTools {
    private final MemoryStoreService memoryStoreService;
    private final StateStoreService stateStoreService;
    private final ExecService execService;
    private final EntityStoreService entityStoreService;
    private final SkillStoreService skillStoreService;
    private final MiniClawKernelService miniClawKernelService;

    private final AnalyticsStoreService analyticsStoreService;

    public MiniClawTools(MemoryStoreService memoryStoreService,
                         StateStoreService stateStoreService,
                         ExecService execService,
                         EntityStoreService entityStoreService,
                         SkillStoreService skillStoreService,
                         MiniClawKernelService miniClawKernelService,
                         AnalyticsStoreService analyticsStoreService) {
        this.memoryStoreService = memoryStoreService;
        this.stateStoreService = stateStoreService;
        this.execService = execService;
        this.entityStoreService = entityStoreService;
        this.skillStoreService = skillStoreService;
        this.miniClawKernelService = miniClawKernelService;
        this.analyticsStoreService = analyticsStoreService;
    }

    @Tool(name = "miniclaw_observe", description = "分析最近日志中的模式")
    public String miniclawObserve() {
        analyticsStoreService.trackTool("miniclaw_observe");
        return miniClawKernelService.buildObserverReport();
    }

    @Tool(name = "miniclaw_distill_suggestion", description = "查看是否建议执行 growup/distill")
    public String miniclawDistillSuggestion() {
        analyticsStoreService.trackTool("miniclaw_distill_suggestion");
        return miniClawKernelService.buildDistillSuggestion();
    }
    @Tool(name = "miniclaw_introspect", description = "查看 MiniClaw 的自我观察结果")
    public String miniclawIntrospect() {
        analyticsStoreService.trackTool("miniclaw_introspect");

        return miniClawKernelService.buildKernelIntrospection();
    }

    @Tool(name = "miniclaw_growup", description = "蒸馏今日日志并写入 REFLECTION.md")
    public String miniclawGrowup() {
        analyticsStoreService.trackTool("miniclaw_growup");

        return miniClawKernelService.performGrowup();
    }
    @Tool(name = "miniclaw_briefing", description = "查看 MiniClaw 的开机摘要")
    public String miniclawBriefing() {
        analyticsStoreService.trackTool("miniclaw_briefing");

        return miniClawKernelService.buildKernelBriefing();
    }


    @Tool(name = "miniclaw_read", description = "读取 MiniClaw 当前上下文")
    public String miniclawRead(@ToolParam(description = "上下文模式：full 或 minimal") String mode) {
        analyticsStoreService.trackTool("miniclaw_read");
        return miniClawKernelService.buildKernelContext(ContextMode.from(mode));
    }

    @Tool(name = "miniclaw_note", description = "往今日日志里追加一条记录")
    public String miniclawNote(@ToolParam(description = "要写入今日日志的文本") String text) {
        analyticsStoreService.trackTool("miniclaw_note");
        memoryStoreService.appendDailyLog(text);
        analyticsStoreService.trackFileChange("memory/" + java.time.LocalDate.now() + ".md");

        return "已写入今日日志：" + text;
    }
    @Tool(name = "miniclaw_update", description = "列出、写入或删除 MiniClaw 管理的文件")
    public String miniclawUpdate(
            @ToolParam(description = "操作类型：list / write / delete") String action,
            @ToolParam(description = "目标文件名。write/delete 时必填") String filename,
            @ToolParam(description = "文件内容。write 时必填") String content
    ) {
        analyticsStoreService.trackTool("miniclaw_update");
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action 不能为空");
        }

        return switch (action) {
            case "list" -> {
                List<String> files = memoryStoreService.listManagedFiles();
                yield files.isEmpty() ? "当前没有文件" : String.join("\n", files);
            }
            case "write" -> {
                requireText(filename, "write 操作必须提供 filename");
                requireText(content, "write 操作必须提供 content");
                analyticsStoreService.trackFileChange(filename);
                yield memoryStoreService.writeManagedFile(filename, content);
            }
            case "delete" -> {
                requireText(filename, "delete 操作必须提供 filename");
                analyticsStoreService.trackFileChange(filename);
                yield memoryStoreService.deleteCustomFile(filename);
            }
            default -> throw new IllegalArgumentException("不支持的 action: " + action);
        };
    }

    @Tool(name = "miniclaw_status", description = "查看 MiniClaw 当前状态")
    public String miniclawStatus() {
        analyticsStoreService.trackTool("miniclaw_status");

        return miniClawKernelService.buildKernelStatus();
    }

    @Tool(name = "miniclaw_overview", description = "查看 MiniClaw 的系统总览")
    public String miniclawOverview() {
        analyticsStoreService.trackTool("miniclaw_overview");

        return miniClawKernelService.buildSystemOverview();
    }
    private void requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
    }

    @Tool(name = "miniclaw_exec", description = "安全执行白名单命令")
    public String miniclawExec(
            @ToolParam(description = "命令名，例如 git/java/mvn/python/where") String command,
            @ToolParam(description = "命令参数列表") List<String> args
    ) {
        analyticsStoreService.trackTool("miniclaw_exec");

        return execService.execute(command, args);
    }

    @Tool(name = "miniclaw_analytics", description = "查看 MiniClaw 的使用分析")
    public String miniclawAnalytics() {
        analyticsStoreService.trackTool("miniclaw_analytics");
        return miniClawKernelService.buildAnalyticsSummary();
    }

    @Tool(name = "miniclaw_entity", description = "管理 MiniClaw 的实体知识图谱")
    public String miniclawEntity(
            @ToolParam(description = "动作：add / query / list / link") String action,
            @ToolParam(description = "实体名称") String name,
            @ToolParam(description = "实体类型：person / project / tool / concept / place / other") String type,
            @ToolParam(description = "实体属性") java.util.Map<String, String> attributes,
            @ToolParam(description = "关系描述") String relation,
            @ToolParam(description = "按类型筛选 list") String filterType,
            @ToolParam(description = "情感标记") String sentiment
    ) {
        analyticsStoreService.trackTool("miniclaw_entity");

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action 不能为空");
        }

        return switch (action) {
            case "add" -> formatEntity(entityStoreService.add(name, type, attributes, sentiment));
            case "query" -> {
                var entity = entityStoreService.query(name);
                yield entity == null ? "未找到实体: " + name : formatEntity(entity);
            }
            case "list" -> {
                var entities = entityStoreService.list(filterType);
                if (entities.isEmpty()) {
                    yield "当前没有实体";
                }
                StringBuilder sb = new StringBuilder("Entities:\n");
                for (var entity : entities) {
                    sb.append("- ").append(entity.getName())
                            .append(" (").append(entity.getType()).append(")")
                            .append(" mentions=").append(entity.getMentionCount())
                            .append("\n");
                }
                yield sb.toString();
            }
            case "link" -> formatEntity(entityStoreService.link(name, relation));
            default -> throw new IllegalArgumentException("不支持的 action: " + action);
        };
    }

    private String formatEntity(Entity entity) {
        return """
            name: %s
            type: %s
            mentionCount: %d
            firstMentioned: %s
            lastMentioned: %s
            sentiment: %s
            attributes: %s
            relations: %s
            """.formatted(
                entity.getName(),
                entity.getType(),
                entity.getMentionCount(),
                entity.getFirstMentioned(),
                entity.getLastMentioned(),
                entity.getSentiment(),
                entity.getAttributes(),
                entity.getRelations()
        );
    }

    @Tool(name = "miniclaw_skill", description = "列出、读取、创建、删除 MiniClaw 技能")
    public String miniclawSkill(
            @ToolParam(description = "动作：list / read / files / read_file / create / delete / write_file") String action,
            @ToolParam(description = "技能名称") String skillName,
            @ToolParam(description = "技能内文件名，例如 references/cheatsheet.md") String fileName,
            @ToolParam(description = "技能描述") String description,
            @ToolParam(description = "技能主文档内容") String content,
            @ToolParam(description = "主工具名") String toolName,
            @ToolParam(description = "主工具描述") String toolDescription,
            @ToolParam(description = "文件工具名") String fileToolName,
            @ToolParam(description = "文件工具描述") String fileToolDescription
    ) {
        analyticsStoreService.trackTool("miniclaw_skill");

        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action 不能为空");
        }

        return switch (action) {
            case "list" -> {
                var skills = skillStoreService.listSkills();
                if (skills.isEmpty()) {
                    yield "当前没有技能";
                }

                StringBuilder sb = new StringBuilder("Skills:\n");
                for (var skill : skills) {
                    sb.append("- ").append(skill.getName())
                            .append(" : ").append(skill.getDescription())
                            .append(" | tool=").append(skill.getToolName())
                            .append(" | fileTool=").append(skill.getFileToolName())
                            .append(" | files=").append(skill.getFiles())
                            .append("\n");
                }
                yield sb.toString();
            }
            case "read" -> {
                requireText(skillName, "read 需要 skillName");
                yield skillStoreService.readSkill(skillName);
            }
            case "files" -> {
                requireText(skillName, "files 需要 skillName");
                var files = skillStoreService.listSkillFiles(skillName);
                if (files.isEmpty()) {
                    yield "该技能下没有文件: " + skillName;
                }
                yield String.join("\n", files);
            }
            case "read_file" -> {
                requireText(skillName, "read_file 需要 skillName");
                requireText(fileName, "read_file 需要 fileName");
                yield skillStoreService.readSkillFile(skillName, fileName);
            }
            case "create" -> {
                requireText(skillName, "create 需要 skillName");
                requireText(description, "create 需要 description");
                String result =  skillStoreService.createSkill(
                        skillName,
                        description,
                        content,
                        toolName,
                        toolDescription,
                        fileToolName,
                        fileToolDescription
                );
                analyticsStoreService.trackFileChange("skills/" + skillName + "/SKILL.md");
                yield withSkillRefreshHint(result);
            }
            case "delete" -> {
                requireText(skillName, "delete 需要 skillName");
                String result = skillStoreService.deleteSkill(skillName);
                analyticsStoreService.trackFileChange("skills/" + skillName);
                yield withSkillRefreshHint(result);
            }
            case "write_file" -> {
                requireText(skillName, "write_file 需要 skillName");
                requireText(fileName, "write_file 需要 fileName");
                requireText(content, "write_file 需要 content");
                String result =  skillStoreService.createSkillFile(skillName, fileName, content);
                analyticsStoreService.trackFileChange("skills/" + skillName + "/" + fileName);
                yield withSkillRefreshHint(result);
            }
            default -> throw new IllegalArgumentException("不支持的 action: " + action);
        };
    }
    private String withSkillRefreshHint(String message) {
        return message + System.lineSeparator()
                + System.lineSeparator()
                + "[提示] 技能目录已更新。"
                + System.lineSeparator()
                + "当前版本的动态技能工具和技能资源在应用启动时注册。"
                + System.lineSeparator()
                + "如果你新增/删除了 skill，或修改了 SKILL.md 中的 tool-name / file-tool-name，请重启服务后再验证工具列表与资源列表。";
    }

    @Tool(name = "miniclaw_archive", description = "归档日志，或查看归档状态")
    public String miniclawArchive(
            @ToolParam(description = "动作：archive_today / archive_latest / summary") String action
    ) {
        analyticsStoreService.trackTool("miniclaw_archive");

        if (action == null || action.isBlank()) {
            action = "summary";
        }

        return switch (action) {
            case "archive_today" -> {
                String result = miniClawKernelService.archiveTodayLog();
                analyticsStoreService.trackFileChange("memory/archived");
                yield result;
            }
            case "archive_latest" -> {
                String result = miniClawKernelService.archiveLatestLog();
                analyticsStoreService.trackFileChange("memory/archived");
                yield result;
            }
            case "summary" -> miniClawKernelService.buildArchiveSummary();
            default -> throw new IllegalArgumentException("不支持的 archive action: " + action);
        };
    }


    @Tool(name = "miniclaw_evolution", description = "查看或执行 MiniClaw 的规则版演化")
    public String miniclawEvolution(
            @ToolParam(description = "动作：report / run") String action
    ) {
        analyticsStoreService.trackTool("miniclaw_evolution");

        if (action == null || action.isBlank()) {
            action = "report";
        }

        return switch (action) {
            case "report" -> miniClawKernelService.buildEvolutionReport();
            case "run" -> {
                String result = miniClawKernelService.performEvolution();
                analyticsStoreService.trackFileChange("EVOLUTION.md");
                analyticsStoreService.trackFileChange("evolution-state.json");
                yield result;
            }
            default -> throw new IllegalArgumentException("不支持的 evolution action: " + action);
        };
    }

    @Tool(name = "miniclaw_dream", description = "查看或执行 MiniClaw 的意义提炼")
    public String miniclawDream(
            @ToolParam(description = "动作：report / run") String action
    ) {
        analyticsStoreService.trackTool("miniclaw_dream");

        if (action == null || action.isBlank()) {
            action = "report";
        }

        return switch (action) {
            case "report" -> miniClawKernelService.buildDreamReport();
            case "run" -> {
                String result = miniClawKernelService.performDream();
                analyticsStoreService.trackFileChange("DREAM.md");
                yield result;
            }
            default -> throw new IllegalArgumentException("不支持的 dream action: " + action);
        };
    }
}

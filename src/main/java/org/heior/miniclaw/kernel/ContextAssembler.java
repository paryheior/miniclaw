package org.heior.miniclaw.kernel;

import org.heior.miniclaw.WorkspaceInfo.WorkspaceInfo;
import org.heior.miniclaw.entity.Entity;
import org.heior.miniclaw.skill.SkillInfo;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class ContextAssembler {

    private static final int FULL_MAX_CHARS = 12000;
    private static final int MINIMAL_MAX_CHARS = 5000;

    public String assemble(ContextMode mode,
                           WorkspaceInfo workspace,
                           String briefing,
                           List<SkillInfo> skills,
                           List<Entity> entities,
                           String identity,
                           String soul,
                           String user,
                           String agents,
                           String memory,
                           String todayLog) {

        List<ContextSection> sections = new ArrayList<>();

        sections.add(new ContextSection("briefing", wrap("BRIEFING", briefing), 10));
        sections.add(new ContextSection("workspace", formatWorkspace(workspace), 9));
        sections.add(new ContextSection("skills", formatSkills(skills, mode == ContextMode.MINIMAL ? 3 : 8), 8));
        sections.add(new ContextSection("entities", formatEntities(entities, mode == ContextMode.MINIMAL ? 3 : 8), 7));

        sections.add(new ContextSection("identity", wrap("IDENTITY.md", identity), 6));
        sections.add(new ContextSection("soul", wrap("SOUL.md", soul), 5));
        sections.add(new ContextSection("user", wrap("USER.md", user), 5));

        if (mode == ContextMode.FULL) {
            sections.add(new ContextSection("agents", wrap("AGENTS.md", agents), 4));
            sections.add(new ContextSection("memory", wrap("MEMORY.md", memory), 3));
            sections.add(new ContextSection("todayLog", wrap("TODAY_LOG", tailLog(todayLog, 20)), 2));
        } else {
            sections.add(new ContextSection("todayLog", wrap("TODAY_LOG", tailLog(todayLog, 8)), 4));
        }

        sections.sort(Comparator.comparingInt(ContextSection::priority).reversed());

        int maxChars = mode == ContextMode.MINIMAL ? MINIMAL_MAX_CHARS : FULL_MAX_CHARS;
        StringBuilder sb = new StringBuilder();
        int used = 0;

        for (ContextSection section : sections) {
            String content = section.content();
            if (content == null || content.isBlank()) {
                continue;
            }

            if (used + content.length() <= maxChars) {
                sb.append(content).append(System.lineSeparator());
                used += content.length();
            } else {
                int remain = maxChars - used;
                if (remain > 200) {
                    sb.append(content, 0, Math.min(remain, content.length()))
                            .append(System.lineSeparator())
                            .append("... [truncated]")
                            .append(System.lineSeparator());
                }
                break;
            }
        }

        return sb.toString();
    }

    private String formatWorkspace(WorkspaceInfo info) {
        if (info == null) {
            return "";
        }

        return """
                ## WORKSPACE
                - path: %s
                - name: %s
                - gitRepo: %s
                - gitBranch: %s
                - gitStatus: %s
                - techStack: %s
                """.formatted(
                info.getPath(),
                info.getName(),
                info.isGitRepo(),
                info.getGitBranch(),
                info.getGitStatus(),
                info.getTechStack()
        );
    }

    private String formatSkills(List<SkillInfo> skills, int limit) {
        StringBuilder sb = new StringBuilder("## SKILLS\n");
        if (skills == null || skills.isEmpty()) {
            sb.append("(no skills)\n");
            return sb.toString();
        }

        for (var skill : skills.stream().limit(limit).toList()) {
            sb.append("- ").append(skill.getName())
                    .append(" : ").append(skill.getDescription())
                    .append(" | tool=").append(skill.getToolName())
                    .append("\n");
        }
        return sb.toString();
    }

    private String formatEntities(List<Entity> entities, int limit) {
        StringBuilder sb = new StringBuilder("## ENTITIES\n");
        if (entities == null || entities.isEmpty()) {
            sb.append("(no entities)\n");
            return sb.toString();
        }

        entities.stream()
                .sorted(Comparator.comparingInt(Entity::getMentionCount).reversed())
                .limit(limit)
                .forEach(entity -> sb.append("- ")
                        .append(entity.getName())
                        .append(" (").append(entity.getType()).append(")")
                        .append(" mentions=").append(entity.getMentionCount())
                        .append("\n"));

        return sb.toString();
    }

    private String wrap(String title, String content) {
        if (content == null || content.isBlank()) {
            return "";
        }
        return "## " + title + "\n" + content.strip() + "\n";
    }

    private String tailLog(String content, int maxLines) {
        if (content == null || content.isBlank()) {
            return "";
        }

        String[] lines = content.split("\\R");
        List<String> kept = new ArrayList<>();
        for (String line : lines) {
            if (line.trim().startsWith("- [")) {
                kept.add(line);
            }
        }

        int from = Math.max(0, kept.size() - maxLines);
        return String.join(System.lineSeparator(), kept.subList(from, kept.size()));
    }
}
package org.heior.miniclaw.skill;


import org.heior.miniclaw.config.MiniClawProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SkillStoreService {
    private final MiniClawProperties properties;
    public SkillStoreService(MiniClawProperties properties) {
        this.properties = properties;
    }

    public Path getSkillsDir() {
        return properties.getHomePath().resolve("skills");
    }
    public List<SkillInfo> listSkills() {
        Path skillsDir = getSkillsDir();
        try {
            Files.createDirectories(skillsDir);

            try (Stream<Path> stream = Files.list(skillsDir)) {
                return stream
                        .filter(Files::isDirectory)
                        .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                        .map(this::toSkillInfo)
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException("列出 skills 失败: " + e.getMessage(), e);
        }
    }
    private SkillInfo toSkillInfo(Path skillDir) {
        SkillInfo info = new SkillInfo();
        info.setName(skillDir.getFileName().toString());

        Path skillMd = skillDir.resolve("SKILL.md");
        String content = Files.exists(skillMd) ? safeRead(skillMd) : "";

        var frontmatter = parseFrontmatter(content);

        String description = frontmatter.getOrDefault("description", extractDescription(content));
        info.setDescription(description);

        String defaultToolName = "skill_" + info.getName().toLowerCase().replace("-", "_") + "_read";
        info.setToolName(frontmatter.getOrDefault("tool-name", defaultToolName));
        info.setToolDescription(frontmatter.getOrDefault(
                "tool-description",
                "Read the SKILL.md content for skill: " + info.getName()
        ));

        String defaultFileToolName = "skill_" + info.getName().toLowerCase().replace("-", "_") + "_read_file";
        info.setFileToolName(frontmatter.getOrDefault("file-tool-name", defaultFileToolName));
        info.setFileToolDescription(frontmatter.getOrDefault(
                "file-tool-description",
                "Read a specific file from skill: " + info.getName()
        ));

        try (Stream<Path> stream = Files.walk(skillDir)) {
            List<String> files = stream
                    .filter(Files::isRegularFile)
                    .map(path -> skillDir.relativize(path).toString().replace("\\", "/"))
                    .sorted()
                    .collect(Collectors.toList());
            info.setFiles(files);
        } catch (IOException e) {
            info.setFiles(new ArrayList<>());
        }

        return info;
    }

    public String readSkill(String skillName) {
        validateSkillName(skillName);
        Path skillFile = getSkillsDir().resolve(skillName).resolve("SKILL.md");
        return readFile(skillFile, "技能不存在或没有 SKILL.md: " + skillName);
    }

    public String readSkillFile(String skillName, String fileName) {
        validateSkillName(skillName);
        validateRelativeFileName(fileName);

        Path file = getSkillsDir().resolve(skillName).resolve(fileName);
        return readFile(file, "技能文件不存在: " + skillName + "/" + fileName);
    }

    public List<String> listSkillFiles(String skillName) {
        validateSkillName(skillName);
        Path skillDir = getSkillsDir().resolve(skillName);

        if (Files.notExists(skillDir) || !Files.isDirectory(skillDir)) {
            throw new IllegalArgumentException("技能不存在: " + skillName);
        }

        try (Stream<Path> stream = Files.walk(skillDir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> skillDir.relativize(path).toString().replace("\\", "/"))
                    .sorted()
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("列出技能文件失败: " + e.getMessage(), e);
        }
    }
    private String readFile(Path file, String errorMessage) {
        try {
            if (Files.notExists(file) || !Files.isRegularFile(file)) {
                throw new IllegalArgumentException(errorMessage);
            }
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取技能文件失败: " + e.getMessage(), e);
        }
    }

    private String safeRead(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "";
        }
    }

    private void validateSkillName(String skillName) {
        if (skillName == null || skillName.isBlank()) {
            throw new IllegalArgumentException("skillName 不能为空");
        }
        if (skillName.contains("..") || skillName.contains("/") || skillName.contains("\\")) {
            throw new IllegalArgumentException("非法 skillName: " + skillName);
        }
    }

    private void validateRelativeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName 不能为空");
        }
        if (fileName.contains("..") || fileName.startsWith("/") || fileName.startsWith("\\")) {
            throw new IllegalArgumentException("非法 fileName: " + fileName);
        }
    }

    /**
     * 先简单支持：
     * 1. frontmatter 里的 description: xxx
     * 2. 否则取正文第一个非空行
     */
    private String extractDescription(String content) {
        if (content == null || content.isBlank()) {
            return "No description";
        }

        String frontmatterDesc = tryExtractFrontmatterDescription(content);
        if (frontmatterDesc != null && !frontmatterDesc.isBlank()) {
            return frontmatterDesc;
        }

        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (trimmed.equals("---")) {
                continue;
            }
            if (trimmed.startsWith("#")) {
                continue;
            }
            return trimmed.length() > 120 ? trimmed.substring(0, 120) : trimmed;
        }

        return "No description";
    }

    private String tryExtractFrontmatterDescription(String content) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "(?s)^---\\R(.*?)\\R---(?:\\R|$)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return null;
        }

        String frontmatter = matcher.group(1);
        for (String line : frontmatter.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("description:")) {
                return trimmed.substring("description:".length()).trim().replace("\"", "");
            }
        }
        return null;
    }
    private java.util.Map<String, String> parseFrontmatter(String content) {
        java.util.Map<String, String> map = new java.util.HashMap<>();

        if (content == null || content.isBlank()) {
            return map;
        }



        content = normalizeEscapedMarkdown(content);

        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(?s)^---\\R(.*?)\\R---(?:\\R|$)");
        java.util.regex.Matcher matcher = pattern.matcher(content);
        if (!matcher.find()) {
            return map;
        }

        String frontmatter = matcher.group(1);
        for (String line : frontmatter.split("\\R")) {
            String trimmed = line.trim();
            int idx = trimmed.indexOf(':');
            if (idx > 0) {
                String key = trimmed.substring(0, idx).trim();
                String value = trimmed.substring(idx + 1).trim().replace("\"", "");
                map.put(key, value);
            }
        }

        return map;
    }

    private String normalizeEscapedMarkdown(String content) {
        return content
                .replace("\\---", "---")
                .replace("\\#", "#")
                .replace("\\- ", "- ");
    }

    public List<String> listSkillNames() {
        return listSkills().stream()
                .map(SkillInfo::getName)
                .toList();
    }

    public List<String> listAllSkillResourceUris() {
        List<String> uris = new ArrayList<>();

        for (var skill : listSkills()) {
            uris.add("miniclaw://skill/" + skill.getName());

            for (String file : skill.getFiles()) {
                if ("SKILL.md".equals(file)) {
                    continue;
                }
                uris.add("miniclaw://skill/" + skill.getName() + "/" + file);
            }
        }

        return uris;
    }

    public String createSkill(String skillName,
                              String description,
                              String mainContent,
                              String toolName,
                              String toolDescription,
                              String fileToolName,
                              String fileToolDescription) {
        validateSkillName(skillName);

        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("description 不能为空");
        }

        Path skillDir = getSkillsDir().resolve(skillName);
        Path skillMd = skillDir.resolve("SKILL.md");

        try {
            Files.createDirectories(getSkillsDir());

            if (Files.exists(skillDir)) {
                throw new IllegalArgumentException("技能已存在: " + skillName);
            }

            Files.createDirectories(skillDir);

            String defaultToolName = "skill_" + skillName.toLowerCase().replace("-", "_") + "_read";
            String defaultFileToolName = "skill_" + skillName.toLowerCase().replace("-", "_") + "_read_file";

            String finalToolName = (toolName == null || toolName.isBlank()) ? defaultToolName : toolName;
            String finalToolDescription = (toolDescription == null || toolDescription.isBlank())
                    ? "读取 " + skillName + " 技能说明"
                    : toolDescription;
            String finalFileToolName = (fileToolName == null || fileToolName.isBlank())
                    ? defaultFileToolName
                    : fileToolName;
            String finalFileToolDescription = (fileToolDescription == null || fileToolDescription.isBlank())
                    ? "读取 " + skillName + " 技能中的指定文件"
                    : fileToolDescription;

            String body = (mainContent == null || mainContent.isBlank())
                    ? "# " + skillName + System.lineSeparator() + System.lineSeparator() + "TODO: add skill content"
                    : mainContent;

            String content = """
                ---
                name: %s
                description: %s
                tool-name: %s
                tool-description: %s
                file-tool-name: %s
                file-tool-description: %s
                ---
                                
                %s
                """.formatted(
                    skillName,
                    description,
                    finalToolName,
                    finalToolDescription,
                    finalFileToolName,
                    finalFileToolDescription,
                    body
            );

            Files.writeString(skillMd, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);

            return "已创建技能: " + skillName;
        } catch (IOException e) {
            throw new RuntimeException("创建技能失败: " + e.getMessage(), e);
        }
    }

    public String deleteSkill(String skillName) {
        validateSkillName(skillName);
        Path skillDir = getSkillsDir().resolve(skillName);
        if (Files.notExists(skillDir) || !Files.isDirectory(skillDir)) {
            throw new IllegalArgumentException("技能不存在: " + skillName);
        }

        try (Stream<Path> stream = Files.walk(skillDir)) {
            stream.sorted(Comparator.reverseOrder())
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException("删除技能文件失败: " + path + ", " + e.getMessage(), e);
                        }
                    });
            return "已删除技能: " + skillName;
        } catch (IOException e) {
            throw new RuntimeException("删除技能失败: " + e.getMessage(), e);
        }
    }

    public String createSkillFile(String skillName, String fileName, String content) {
        validateSkillName(skillName);
        validateRelativeFileName(fileName);
        if (content == null) {
            throw new IllegalArgumentException("content 不能为空");
        }
        Path skillDir = getSkillsDir().resolve(skillName);
        if (Files.notExists(skillDir) || !Files.isDirectory(skillDir)) {
            throw new IllegalArgumentException("技能不存在: " + skillName);
        }

        Path file = skillDir.resolve(fileName);

        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return "已写入技能文件: " + skillName + "/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("写入技能文件失败: " + e.getMessage(), e);
        }
    }
}

package org.heior.miniclaw.memory;

import org.heior.miniclaw.WorkspaceInfo.WorkspaceService;
import org.heior.miniclaw.config.MiniClawProperties;
import org.heior.miniclaw.skill.SkillStoreService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class MemoryStoreService {
    private static final List<String> CORE_FILES = List.of(
            "IDENTITY.md",
            "SOUL.md",
            "USER.md",
            "AGENTS.md",
            "MEMORY.md",
            "TOOLS.md",
            "HORIZONS.md",
            "REFLECTION.md",
            "HEARTBEAT.md",
            "RIBOSOME.json"
    );
    private final MiniClawProperties properties;
    private final WorkspaceService workspaceService;
    private final SkillStoreService skillStoreService;
    public MemoryStoreService(MiniClawProperties properties,
                              WorkspaceService workspaceService,
                              SkillStoreService skillStoreService) {
        this.properties = properties;
        this.workspaceService = workspaceService;
        this.skillStoreService = skillStoreService;
    }

    public Path getHome() {
        return properties.getHomePath();
    }

    public Path getMemoryDir() {
        return getHome().resolve("memory");
    }

    public Path getArchivedDir() {
        return getMemoryDir().resolve("archived");
    }

    public Path getTodayLogPath() {
        String today = LocalDate.now().toString();
        return getMemoryDir().resolve(today + ".md");
    }

    public String readFile(String filename) {
        validateCoreFilename(filename);
        Path path = getHome().resolve(filename);
        return readPath(path);
    }
    public String readDailyLog() {
        return readPath(getTodayLogPath());
    }

    public void appendDailyLog(String text) {
        try {
            Files.createDirectories(getMemoryDir());

            Path todayLog = getTodayLogPath();
            if (Files.notExists(todayLog)) {
                Files.writeString(
                        todayLog,
                        "# Daily Log - " + LocalDate.now() + System.lineSeparator(),
                        StandardCharsets.UTF_8
                );
            }

            String now = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            String line = "- [" + now + "] " + text + System.lineSeparator();

            Files.writeString(
                    todayLog,
                    line,
                    StandardCharsets.UTF_8,
                    java.nio.file.StandardOpenOption.CREATE,
                    java.nio.file.StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("写入今日日志失败: " + e.getMessage(), e);
        }
    }

    public String buildContext() {
        StringBuilder sb = new StringBuilder();
        var workspace = workspaceService.detect();
        sb.append(workspaceService.formatForContext(workspace)).append(System.lineSeparator());
        appendSkillsIndex(sb);
        appendSection(sb, "IDENTITY.md", readFile("IDENTITY.md"));
        appendSection(sb, "SOUL.md", readFile("SOUL.md"));
        appendSection(sb, "USER.md", readFile("USER.md"));
        appendSection(sb, "AGENTS.md", readFile("AGENTS.md"));
        appendSection(sb, "MEMORY.md", readFile("MEMORY.md"));
        appendSection(sb, "TODAY_LOG", readDailyLog());

        return sb.toString();
    }

    private void appendSkillsIndex(StringBuilder sb) {
        var skills = skillStoreService.listSkills();

        sb.append("## SKILLS").append(System.lineSeparator());

        if (skills.isEmpty()) {
            sb.append("(no skills)").append(System.lineSeparator()).append(System.lineSeparator());
            return;
        }

        for (var skill : skills) {
            sb.append("- ").append(skill.getName())
                    .append(" : ").append(skill.getDescription());

            if (skill.getToolName() != null && !skill.getToolName().isBlank()) {
                sb.append(" | tool=").append(skill.getToolName());
            }

            if (skill.getFileToolName() != null && !skill.getFileToolName().isBlank()) {
                sb.append(" | fileTool=").append(skill.getFileToolName());
            }

            sb.append(System.lineSeparator());
        }

        sb.append(System.lineSeparator());
    }

    public List<String> listManagedFiles() {
        try (Stream<Path> stream = Files.list(getHome())) {
            return stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .sorted(Comparator.naturalOrder())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("列出文件失败： " + e.getMessage(), e);
        }
    }

    public String writeManagedFile(String filename, String content) {
        validateCoreFilename(filename);
        if(content == null) throw new IllegalArgumentException("内容 不能为空");
        Path path = getHome().resolve(filename);
        try{
            Files.createDirectories(getHome());
            boolean existed = Files.exists(path);
            if (existed) {
                Path backup = getHome().resolve(filename + ".bak");
                Files.copy(path, backup, StandardCopyOption.REPLACE_EXISTING);
            }
            Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
            if(existed) return "已更新文件：" + filename;
            return "已创建文件：" + filename;
        } catch (IOException e) {
            throw new RuntimeException("写入文件失败: " + filename + ", " + e.getMessage(), e);
        }
    }

    public String deleteCustomFile(String filename) {
        validateCoreFilename(filename);
        if(isCoreFile(filename)) throw new IllegalArgumentException("核心文件不能被删除!");
        Path path = getHome().resolve(filename);
        try {
            boolean deleted = Files.deleteIfExists(path);
            return deleted ? "已删除文件: " + filename : "文件不存在: " + filename;
        } catch (IOException e) {
            throw new RuntimeException("删除文件失败: " + filename + ", " + e.getMessage(), e);
        }
    }

    public boolean isCoreFile(String filename) {
        return CORE_FILES.contains(filename);
    }

    private void appendSection(StringBuilder sb, String name, String content) {
        sb.append("## ").append(name).append(System.lineSeparator());
        if(content == null || content.isBlank()) {
            sb.append("(empty)").append(System.lineSeparator());
        } else {
            sb.append(content.strip()).append(System.lineSeparator());
        }
        sb.append(System.lineSeparator());
    }

    private String readPath(Path path) {
        try {
            if(Files.notExists(path)) return "";
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取文件失败: " + path + ", " + e.getMessage(), e);
        }
    }
    private void validateCoreFilename(String filename) {
        if(!CORE_FILES.contains(filename)) {
            throw new IllegalArgumentException("！不允许读取的文件：" + filename);
        }
    }

    public void validateManagedFilename(String filename) {
        if(filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("filename is empty!");
        }
        if(filename.contains("..") || filename.contains("/") || filename.contains("\\")) {
            throw new IllegalArgumentException("filename is invalid: " + filename);
        }
        if(!filename.endsWith(".md") && !filename.endsWith(".json")) {
            throw new IllegalArgumentException(".md or .json is required! filename is invalid: " + filename);
        }
    }


    public int countTodayLogEntries() {
        String content = readDailyLog();
        if(content == null || content.isBlank()) return 0;
        int count = 0;
        for(String line : content.split("\\R")) {
            String trimmed = line.trim();
            if(trimmed.startsWith("- [")) count++;
        }
        return count;
    }

    public List<String> readTodayLogEntries() {
        String content = readDailyLog();
        List<String> entries = new ArrayList<>();
        if(content == null || content.isBlank()) return entries;
        for (String line : content.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- [")) {
                entries.add(trimmed);
            }
        }
        return entries;
    }
    public void appendReflection(String text) {
        Path reflectionFile = getHome().resolve("REFLECTION.md");
        try {
            if (Files.notExists(reflectionFile)) {
                Files.writeString(reflectionFile, "# Reflection" + System.lineSeparator(), StandardCharsets.UTF_8);
            }

            Files.writeString(
                    reflectionFile,
                    text + System.lineSeparator(),
                    StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.APPEND
            );
        } catch (IOException e) {
            throw new RuntimeException("写入 REFLECTION.md 失败: " + e.getMessage(), e);
        }
    }

    public Path findLatestNonEmptyDailyLog() {
        Path memoryDir = getMemoryDir();
        try {
            Files.createDirectories(memoryDir);

            try (var stream = Files.list(memoryDir)) {
                return stream
                        .filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".md"))
                        .filter(path -> !"archived".equalsIgnoreCase(path.getFileName().toString()))
                        .sorted(java.util.Comparator.comparing(
                                (Path path) -> path.getFileName().toString()
                        ).reversed())
                        .filter(path -> {
                            try {
                                String content = Files.readString(path, StandardCharsets.UTF_8);
                                return content != null && content.lines().anyMatch(line -> line.trim().startsWith("- ["));
                            } catch (IOException e) {
                                return false;
                            }
                        })
                        .findFirst()
                        .orElse(null);
            }
        } catch (IOException e) {
            throw new RuntimeException("查找最近非空日志失败: " + e.getMessage(), e);
        }
    }

    public List<String> readLogEntries(Path logPath) {
        List<String> entries = new ArrayList<>();

        if (logPath == null || Files.notExists(logPath)) {
            return entries;
        }

        try {
            String content = Files.readString(logPath, StandardCharsets.UTF_8);
            if (content == null || content.isBlank()) {
                return entries;
            }

            for (String line : content.split("\\R")) {
                String trimmed = line.trim();
                if (trimmed.startsWith("- [")) {
                    entries.add(trimmed);
                }
            }
            return entries;
        } catch (IOException e) {
            throw new RuntimeException("读取日志失败: " + logPath + ", " + e.getMessage(), e);
        }
    }

    public List<String> readLatestNonEmptyLogEntries() {
        Path latest = findLatestNonEmptyDailyLog();
        return readLogEntries(latest);
    }
}

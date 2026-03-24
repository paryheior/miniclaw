package org.heior.miniclaw.archive;

import org.heior.miniclaw.config.MiniClawProperties;
import org.heior.miniclaw.memory.MemoryStoreService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ArchiveService {
    private final MiniClawProperties properties;
    private final MemoryStoreService memoryStoreService;
    public ArchiveService(MiniClawProperties properties, MemoryStoreService memoryStoreService) {
        this.properties = properties;
        this.memoryStoreService = memoryStoreService;
    }

    public Path getMemoryDir() {
        return properties.getHomePath().resolve("memory");
    }

    public Path getArchivedDir() {
        return getMemoryDir().resolve("archive");
    }
    public String archiveTodayLog() {
        return archiveLog(memoryStoreService.getTodayLogPath(), false);
    }

    public String archiveLatestNonEmptyLog() {
        Path latest = memoryStoreService.findLatestNonEmptyDailyLog();
        if (latest == null) {
            return "没有可归档的非空日志。";
        }
        return archiveLog(latest, true);
    }
    public List<String> listArchivedLogs() {
        try {
            Files.createDirectories(getArchivedDir());

            try (Stream<Path> stream = Files.list(getArchivedDir())) {
                return stream
                        .filter(Files::isRegularFile)
                        .map(path -> path.getFileName().toString())
                        .sorted(Comparator.reverseOrder())
                        .collect(Collectors.toList());
            }
        } catch (IOException e) {
            throw new RuntimeException("列出 archived logs 失败: " + e.getMessage(), e);
        }
    }

    public String readArchivedLog(String fileName) {
        validateArchiveFileName(fileName);

        Path file = getArchivedDir().resolve(fileName);
        try {
            if (Files.notExists(file) || !Files.isRegularFile(file)) {
                throw new IllegalArgumentException("归档日志不存在: " + fileName);
            }
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException("读取归档日志失败: " + e.getMessage(), e);
        }
    }

    private String archiveLog(Path source, boolean fallbackMode) {
        try {
            Files.createDirectories(getArchivedDir());

            if (source == null || Files.notExists(source) || !Files.isRegularFile(source)) {
                return fallbackMode ? "没有可归档的非空日志。" : "今日日志不存在，无法归档。";
            }

            String fileName = source.getFileName().toString();
            Path target = getArchivedDir().resolve(fileName);

            if (Files.exists(target)) {
                target = resolveNonConflictTarget(fileName);
            }

            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

            return "归档完成: " + source.getFileName() + " -> archived/" + target.getFileName();
        } catch (IOException e) {
            throw new RuntimeException("归档日志失败: " + e.getMessage(), e);
        }
    }

    private Path resolveNonConflictTarget(String originalFileName) throws IOException {
        String base = originalFileName;
        String suffix = "";
        int dot = originalFileName.lastIndexOf('.');
        if (dot > 0) {
            base = originalFileName.substring(0, dot);
            suffix = originalFileName.substring(dot);
        }

        int i = 1;
        while (true) {
            Path candidate = getArchivedDir().resolve(base + "-" + i + suffix);
            if (Files.notExists(candidate)) {
                return candidate;
            }
            i++;
        }
    }

    private void validateArchiveFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("fileName 不能为空");
        }
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            throw new IllegalArgumentException("非法 fileName: " + fileName);
        }
        if (!fileName.endsWith(".md")) {
            throw new IllegalArgumentException("归档日志必须是 .md 文件: " + fileName);
        }
    }
}

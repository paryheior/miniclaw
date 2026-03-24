package org.heior.miniclaw.WorkspaceInfo;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
@Service
public class WorkspaceService {

    public WorkspaceInfo detect() {
        Path cwd = Path.of(System.getProperty("user.dir"));
        WorkspaceInfo info = new WorkspaceInfo();
        info.setPath(cwd.toAbsolutePath().toString());
        info.setName(cwd.getFileName() != null ? cwd.getFileName().toString() : cwd.toString());

        info.setTechStack(detectTechStack(cwd));
        fillGitInfo(cwd, info);

        return info;
    }

    private List<String> detectTechStack(Path cwd) {
        List<String> stack = new ArrayList<>();

        if (Files.exists(cwd.resolve("pom.xml"))) stack.add("Maven");
        if (Files.exists(cwd.resolve("build.gradle")) || Files.exists(cwd.resolve("build.gradle.kts")))
            stack.add("Gradle");
        if (Files.exists(cwd.resolve("package.json"))) stack.add("Node.js");
        if (Files.exists(cwd.resolve("tsconfig.json"))) stack.add("TypeScript");
        if (Files.exists(cwd.resolve("requirements.txt")) || Files.exists(cwd.resolve("pyproject.toml")))
            stack.add("Python");
        if (Files.exists(cwd.resolve("Cargo.toml"))) stack.add("Rust");
        if (Files.exists(cwd.resolve("go.mod"))) stack.add("Go");
        if (Files.exists(cwd.resolve("docker-compose.yml")) || Files.exists(cwd.resolve("compose.yml")))
            stack.add("Docker");

        return stack;
    }

    private void fillGitInfo(Path cwd, WorkspaceInfo info) {
        try {
            String inside = execChecked(cwd, "git", "rev-parse", "--is-inside-work-tree").trim();
            if (!"true".equalsIgnoreCase(inside)) {
                throw new RuntimeException("not a git repo");
            }

            info.setGitRepo(true);

            String branch = execChecked(cwd, "git", "branch", "--show-current").trim();
            info.setGitBranch(branch.isBlank() ? "(detached or unknown)" : branch);

            String status = execChecked(cwd, "git", "status", "--short").trim();
            info.setGitStatus(status.isBlank() ? "clean" : "dirty");

        } catch (Exception e) {
            info.setGitRepo(false);
            info.setGitBranch("(not a git repo)");
            info.setGitStatus("(not a git repo)");
        }
    }

    private String execChecked(Path cwd, String... command) throws Exception {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(cwd.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        boolean finished = process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("command timeout");
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
            }
        }

        int exitCode = process.exitValue();
        String output = sb.toString().trim();

        if (exitCode != 0) {
            throw new RuntimeException(output.isBlank()
                    ? "command failed: " + String.join(" ", command)
                    : output);
        }

        return output;
    }

    public String formatForContext(WorkspaceInfo info) {
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
                info.getTechStack().isEmpty() ? "[]" : info.getTechStack()
        );
    }
}
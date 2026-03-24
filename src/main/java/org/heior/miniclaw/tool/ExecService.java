package org.heior.miniclaw.tool;

import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class ExecService {

    private static final Set<String> ALLOWED_COMMANDS = Set.of(
            "git",
            "java",
            "mvn.cmd",
            "python",
            "python3",
            "where"
    );
    private static final int MAX_OUTPUT_CHARS = 8000;
    private static final long TIMEOUT_SECONDS = 10;

    public String execute(String command, List<String> args) {
        validate(command, args);
        List<String> fullCommand = new ArrayList<>();
        fullCommand.add(command);

        if(args != null) fullCommand.addAll(args);
        try{
            ProcessBuilder processBuilder = new ProcessBuilder(fullCommand);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if(!finished) {
                process.destroyForcibly();
                return """
                        command %s
                        exitCode: -1
                        output:
                        Process timed out after %d seconds
                        """.formatted(command, TIMEOUT_SECONDS);
            }
            String output = readProcessOutput(process);
            int exitCode = process.exitValue();
            return """
                    command: %s
                    exitCode: %d
                    output:
                    %s
                    """.formatted(command, exitCode, output);
        } catch (Exception e) {
            throw new RuntimeException("执行命令失败: " + e.getMessage(), e);
        }
    }

    private void validate(String command, List<String> args) {
        if(command == null || command.isBlank()) {
            throw new IllegalArgumentException("command is null or blank");
        }
        if(!ALLOWED_COMMANDS.contains(command)) {
            throw new IllegalArgumentException("command is not allowed");
        }
        if(args == null || args.isEmpty()) {
            return ;
        }

        for(String arg : args) {
            if(arg == null || arg.isBlank()) continue;
            if (arg.contains(";") || arg.contains("&") || arg.contains("|")
                    || arg.contains(">") || arg.contains("<")
                    || arg.contains("`")) {
                throw new IllegalArgumentException("检测到危险参数: " + arg);
            }
        }
    }
    private String readProcessOutput(Process process) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), Charset.defaultCharset()))) {
            String line;
            while((line = reader.readLine()) != null) {
                sb.append(line).append(System.lineSeparator());
                if(sb.length() > MAX_OUTPUT_CHARS) {
                    sb.setLength(MAX_OUTPUT_CHARS);
                    sb.append(System.lineSeparator()).append("... [output truncated]");
                    break;
                }
            }
        }
        return sb.toString().strip();
    }


}

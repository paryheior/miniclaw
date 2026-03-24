package org.heior.miniclaw.memory;


import org.heior.miniclaw.config.MiniClawProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;

@Service
public class TemplateBootstrapService implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(TemplateBootstrapService.class);

    private final MiniClawProperties properties;
    private final PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    public TemplateBootstrapService(MiniClawProperties properties) {
        this.properties = properties;
    }
    public void run(ApplicationArguments args) throws Exception {
        if(!properties.isBootstrapEnabled()) {
            log.info("MiniClaw bootstrap disabled");
            return;
        }
        bootstrap();
    }

    public void bootstrap() throws IOException {
        Path home = properties.getHomePath();
        createBaseDirs(home);
        copyRootTemplates(home);
        createExtraDirs(home);
        ensureTodayMemory(home);
        log.info("MiniClaw home initialized at: {}", home);
    }

    private void createBaseDirs(Path home) throws IOException {
        Files.createDirectories(home);
    }

    private void createExtraDirs(Path home) throws IOException {
        Files.createDirectories(home.resolve("memory"));
        Files.createDirectories(home.resolve("skills"));
        Files.createDirectories(home.resolve("memory").resolve("archived"));

    }

    private void copyRootTemplates(Path home) throws IOException {
        Resource[] resources = resolver.getResources("classpath*:templates/miniclaw/*");
        Arrays.stream(resources)
                .filter(Resource::isReadable)
                .filter(resource -> {
                    try {
                        String filename = resource.getFilename();
                        return filename != null && !resource.getURL().toString().endsWith("/skills");

                    } catch (IOException e) {
                        return false;
                    }
                }).forEach(resource -> copyIfAbsent(resource, home));
    }
    private void copyIfAbsent(Resource resource, Path targetDir) {
        try {
            String filename = resource.getFilename();
            if (filename == null) {
                return;
            }

            Path target = targetDir.resolve(filename);
            if (Files.exists(target)) {
                log.debug("Skip existing template: {}", target);
                return;
            }

            try (InputStream in = resource.getInputStream()) {
                Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Copied template: {}", target);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to copy template: " + resource, e);
        }
    }

    private void ensureTodayMemory(Path home) throws IOException {
        String today = java.time.LocalDate.now().toString();
        Path todayFile = home.resolve("memory").resolve(today + ".md");
        if (Files.notExists(todayFile)) {
            Files.writeString(todayFile, "# Daily Log - " + today + System.lineSeparator());
        }
    }

}

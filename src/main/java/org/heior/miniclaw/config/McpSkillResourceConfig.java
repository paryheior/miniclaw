package org.heior.miniclaw.config;

import io.modelcontextprotocol.server.McpServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.heior.miniclaw.skill.SkillStoreService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class McpSkillResourceConfig {

    @Bean
    public List<McpServerFeatures.SyncResourceSpecification> miniClawSkillResources(SkillStoreService skillStoreService) {
        List<McpServerFeatures.SyncResourceSpecification> specs = new ArrayList<>();
        for(var skill : skillStoreService.listSkills()) {
            String skillName = skill.getName();
            // 1) 主资源：miniclaw://skill/<skillName> -> 读取 SKILL.md
            String prefixPath = "miniclaw://skill/";
            var mainResource = McpSchema.Resource.builder()
                    .uri(prefixPath + skillName)
                    .name("skill-" + skillName)
                    .title("Skill: " + skillName)
                    .description("Main skill document for " + skillName)
                    .mimeType("text/markdown")
                    .build();
            var mainSpec = new McpServerFeatures.SyncResourceSpecification(
                    mainResource,
                    (exchange, request) -> new McpSchema.ReadResourceResult(
                            List.of(new McpSchema.TextResourceContents(
                                    request.uri(),
                                    "text/markdown",
                                    skillStoreService.readSkill(skillName)
                            ))
                    )
            );
            specs.add(mainSpec);
            // 2) 子文件资源：miniclaw://skill/<skillName>/<filePath>
            for(String file:skill.getFiles()) {
                if("SKILL.md".equals(file)) continue;
                String uri = prefixPath + skillName + "/" + file;
                var fileResource = McpSchema.Resource.builder()
                    .uri(uri)
                    .name("skill-" + skillName + "-" + sanitizeName(file))
                    .title("Skill File: " + skillName + "/" + file)
                    .mimeType(detectMimeType(file))
                    .build();
                var fileSpec = new McpServerFeatures.SyncResourceSpecification(
                        fileResource,
                        (exchange, request) -> new McpSchema.ReadResourceResult(
                                List.of(new McpSchema.TextResourceContents(
                                        request.uri(),
                                        detectMimeType(file),
                                        skillStoreService.readSkillFile(skillName, file)
                                ))
                        )
                );
                specs.add(fileSpec);
            }
        }
        return specs;
    }
    private String sanitizeName(String file) {
        return file.toLowerCase()
                .replace("/", "-")
                .replace("\\", "-")
                .replace(".", "-")
                .replace(" ", "-");
    }

    private String detectMimeType(String file) {
        if (file.endsWith(".md")) {
            return "text/markdown";
        }
        if (file.endsWith(".json")) {
            return "application/json";
        }
        return "text/plain";
    }
}

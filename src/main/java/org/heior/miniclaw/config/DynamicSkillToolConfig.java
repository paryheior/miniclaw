package org.heior.miniclaw.config;
import org.heior.miniclaw.skill.SkillFileReadRequest;
import org.heior.miniclaw.skill.SkillStoreService;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;
@Configuration
public class DynamicSkillToolConfig {

    @Bean
    public ToolCallbackProvider dynamicSkillReadToolProvider(SkillStoreService skillStoreService) {
        List<ToolCallback> tools = new ArrayList<>();

        for (var skill : skillStoreService.listSkills()) {
            String skillName = skill.getName();

            ToolCallback readSkillTool = FunctionToolCallback
                    .builder(skill.getToolName(), () -> skillStoreService.readSkill(skillName))
                    .description(skill.getToolDescription())
                    .inputType(Void.class)
                    .build();

            tools.add(readSkillTool);
        }

        return ToolCallbackProvider.from(tools);
    }

    @Bean
    public ToolCallbackProvider dynamicSkillReadFileToolProvider(SkillStoreService skillStoreService) {
        List<ToolCallback> tools = new ArrayList<>();

        for (var skill : skillStoreService.listSkills()) {
            String skillName = skill.getName();

            ToolCallback readFileTool = FunctionToolCallback
                    .builder(
                            skill.getFileToolName(),
                            (SkillFileReadRequest request) -> skillStoreService.readSkillFile(skillName, request.fileName())
                    )
                    .description(skill.getFileToolDescription())
                    .inputType(SkillFileReadRequest.class)
                    .build();

            tools.add(readFileTool);
        }

        return ToolCallbackProvider.from(tools);
    }
}
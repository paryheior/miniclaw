package org.heior.miniclaw.debug;


import org.heior.miniclaw.brain.MiniClawBrainService;
import org.heior.miniclaw.kernel.ContextMode;
import org.heior.miniclaw.kernel.MiniClawKernelService;
import org.heior.miniclaw.scheduler.HeartbeatScheduler;
import org.heior.miniclaw.skill.SkillStoreService;
import org.heior.miniclaw.tool.MiniClawTools;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/debug/miniclaw")
public class DebugController {
    private final MiniClawTools miniClawTools;
    private final HeartbeatScheduler heartbeatScheduler;
    private final SkillStoreService skillStoreService;
    private final MiniClawKernelService miniClawKernelService;
    private final MiniClawBrainService miniClawBrainService;
    public DebugController(MiniClawTools miniClawTools,
                           HeartbeatScheduler heartbeatScheduler,
                           SkillStoreService skillStoreService,
                           MiniClawKernelService miniClawKernelService,
                           MiniClawBrainService brainService) {
        this.miniClawTools = miniClawTools;
        this.heartbeatScheduler = heartbeatScheduler;
        this.skillStoreService = skillStoreService;
        this.miniClawKernelService = miniClawKernelService;
        this.miniClawBrainService = brainService;
    }

    /**
     * Handles the creation of a note with the provided text.
     *
     * @param text the text content of the note to be created
     * @return the result of the note creation process, typically a success message or an error message
     */
//    @GetMapping("/read")
//    public String read() {
//        return miniClawTools.miniclawRead();
//    }

    @PostMapping("/chat")
    public String chat(@RequestParam String message,
                       @RequestParam(required = false) String mode) {
        return miniClawBrainService.chat(message, mode);
    }

    @PostMapping("/note")
    public String note(@RequestParam String text) {
        return miniClawTools.miniclawNote(text);
    }

    @GetMapping("/files")
    public String files() {
        return miniClawTools.miniclawUpdate("list", null, null);
    }

    @PostMapping("/write")
    public String write(@RequestParam String filename, @RequestBody String content) {
        return miniClawTools.miniclawUpdate("write", filename, content);
    }

    @DeleteMapping("/delete")
    public String delete(@RequestParam String filename) {
        return miniClawTools.miniclawUpdate("delete", filename, null);
    }

    @GetMapping("/status")
    public String status() {
        return miniClawTools.miniclawStatus();
    }

    @PostMapping("/heartbeat")
    public String forceHeartbeat() {
        heartbeatScheduler.heartbeat();
        return "heartbeat done";
    }

    @PostMapping("/exec")
    public String exec(@RequestParam String command, @RequestBody(required = false) List<String> args) {
        return miniClawTools.miniclawExec(command, args);
    }


    @PostMapping("/exec-simple")
    public String execSimple(@RequestParam String command,
                             @RequestParam(required = false) List<String> args) {
        return miniClawTools.miniclawExec(command, args);
    }

    @PostMapping("/entity/add")
    public String addEntity(@RequestParam String name,
                            @RequestParam String type,
                            @RequestBody(required = false) java.util.Map<String, String> attributes,
                            @RequestParam(required = false) String sentiment) {
        return miniClawTools.miniclawEntity("add", name, type, attributes, null, null, sentiment);
    }

    @GetMapping("/entity/query")
    public String queryEntity(@RequestParam String name) {
        return miniClawTools.miniclawEntity("query", name, null, null, null, null, null);
    }

    @GetMapping("/entity/list")
    public String listEntity(@RequestParam(required = false) String filterType) {
        return miniClawTools.miniclawEntity("list", null, null, null, null, filterType, null);
    }

    @PostMapping("/entity/link")
    public String linkEntity(@RequestParam String name,
                             @RequestParam String relation) {
        return miniClawTools.miniclawEntity("link", name, null, null, relation, null, null);
    }

    @GetMapping("/skill/list")
    public String listSkills() {
        return miniClawTools.miniclawSkill("list", null, null, null, null, null, null, null, null);
    }

    @GetMapping("/skill/read")
    public String readSkill(@RequestParam String skillName) {
        return miniClawTools.miniclawSkill("read", skillName, null,null, null, null, null, null, null);
    }

    @GetMapping("/skill/files")
    public String listSkillFiles(@RequestParam String skillName) {
        return miniClawTools.miniclawSkill("files", skillName, null,null, null, null, null, null, null);
    }

    @GetMapping("/skill/read-file")
    public String readSkillFile(@RequestParam String skillName,
                                @RequestParam String fileName) {
        return miniClawTools.miniclawSkill("read_file", skillName, fileName,null, null, null, null, null, null);
    }

    @GetMapping("/evolution")
    public String evolutionReport() {
        return miniClawTools.miniclawEvolution("report");
    }

    @PostMapping("/evolution/run")
    public String evolutionRun() {
        return miniClawTools.miniclawEvolution("run");
    }

    @GetMapping("/skill/resources")
    public java.util.List<String> skillResources() {
        return skillStoreService.listAllSkillResourceUris();
    }
    @GetMapping("/skill/debug")
    public String debugSkill(@RequestParam String skillName) {
        var skills = skillStoreService.listSkills();
        for (var skill : skills) {
            if (skill.getName().equalsIgnoreCase(skillName)) {
                return """
                    name=%s
                    description=%s
                    toolName=%s
                    toolDescription=%s
                    fileToolName=%s
                    fileToolDescription=%s
                    files=%s
                    """.formatted(
                        skill.getName(),
                        skill.getDescription(),
                        skill.getToolName(),
                        skill.getToolDescription(),
                        skill.getFileToolName(),
                        skill.getFileToolDescription(),
                        skill.getFiles()
                );
            }
        }
        return "skill not found: " + skillName;
    }

    @GetMapping("/dream")
    public String dreamReport() {
        return miniClawTools.miniclawDream("report");
    }

    @PostMapping("/dream/run")
    public String dreamRun() {
        return miniClawTools.miniclawDream("run");
    }

    @PostMapping("/skill/create")
    public String createSkill(@RequestParam String skillName,
                              @RequestParam String description,
                              @RequestBody(required = false) String content) {
        return miniClawTools.miniclawSkill(
                "create",
                skillName,
                null,
                description,
                content,
                null,
                null,
                null,
                null
        );
    }

    @DeleteMapping("/skill/delete")
    public String deleteSkill(@RequestParam String skillName) {
        return miniClawTools.miniclawSkill(
                "delete",
                skillName,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    @PostMapping("/skill/write-file")
    public String writeSkillFile(@RequestParam String skillName,
                                 @RequestParam String fileName,
                                 @RequestBody String content) {
        return miniClawTools.miniclawSkill(
                "write_file",
                skillName,
                fileName,
                null,
                content,
                null,
                null,
                null,
                null
        );
    }

    @GetMapping("/skill/overview")
    public String skillOverview() {
        StringBuilder sb = new StringBuilder();

        sb.append("== Skill Files ==\n");
        sb.append(miniClawTools.miniclawSkill("list", null, null, null, null, null, null, null, null));
        sb.append("\n\n");

        sb.append("== Registered Tools ==\n");
        sb.append("请访问 /debug/miniclaw/tools 查看动态工具列表\n\n");

        sb.append("== Registered Skill Resources ==\n");
        sb.append(skillStoreService.listAllSkillResourceUris());

        return sb.toString();
    }


    @GetMapping("/kernel/status")
    public String kernelStatus() {
        return miniClawKernelService.buildKernelStatus();
    }

    @GetMapping("/kernel/overview")
    public String kernelOverview() {
        return miniClawKernelService.buildSystemOverview();
    }
    @GetMapping("/overview")
    public String overview() {
        return miniClawTools.miniclawOverview();
    }

    @GetMapping("/briefing")
    public String briefing() {
        return miniClawTools.miniclawBriefing();
    }

    @GetMapping("/introspect")
    public String introspect() {
        return miniClawTools.miniclawIntrospect();
    }

    @PostMapping("/growup")
    public String growup() {
        return miniClawTools.miniclawGrowup();
    }

    @GetMapping("/observe")
    public String observe() {
        return miniClawTools.miniclawObserve();
    }

    @GetMapping("/distill-suggestion")
    public String distillSuggestion() {
        return miniClawTools.miniclawDistillSuggestion();
    }
    @GetMapping("/analytics")
    public String analytics() {
        return miniClawTools.miniclawAnalytics();
    }
    @GetMapping("/briefing-v2")
    public String briefingV2() {
        return miniClawTools.miniclawBriefing();
    }

    /**
     * Handles a GET request to build and return a kernel context based on the provided mode.
     *
     * @param mode the mode to determine the type of context to build. It is optional and if not provided,
     *             a default context mode will be used.
     * @return the built kernel context as a String.
     */
    @GetMapping("/context")
    public String context(@RequestParam(required = false) String mode) {
        return miniClawKernelService.buildKernelContext(ContextMode.from(mode));
    }

    @GetMapping("/archive")
    public String archiveSummary() {
        return miniClawTools.miniclawArchive("summary");
    }

    @PostMapping("/archive/today")
    public String archiveToday() {
        return miniClawTools.miniclawArchive("archive_today");
    }

    @PostMapping("/archive/latest")
    public String archiveLatest() {
        return miniClawTools.miniclawArchive("archive_latest");
    }
}


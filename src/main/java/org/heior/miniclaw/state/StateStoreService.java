package org.heior.miniclaw.state;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.heior.miniclaw.config.MiniClawProperties;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.OffsetDateTime;
@Service
public class StateStoreService {
    private final MiniClawProperties properties;
    private final ObjectMapper objectMapper;
    public StateStoreService(MiniClawProperties miniClawProperties, ObjectMapper objectMapper) {
        this.properties = miniClawProperties;
        this.objectMapper = objectMapper;
    }

    public MiniClawState markDistilled() {
        MiniClawState state = load();
        state.setLastDistill(java.time.OffsetDateTime.now().toString());
        state.setNeedsDistill(false);
        save(state);
        return state;
    }

    public MiniClawState load() {
        Path path = getStatePath();
        try {
            if (Files.notExists(path)) {
                MiniClawState state = new MiniClawState();
                save(state);
                return state;
            }
            return objectMapper.readValue(path.toFile(), MiniClawState.class);
        } catch (IOException e) {
            throw new RuntimeException("读取 state.json 文件失败" + e.getMessage(), e);
        }

    }

    public void save(MiniClawState state) {
        try {
            Files.createDirectories(properties.getHomePath());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(getStatePath().toFile(), state);
        } catch (IOException e) {
            throw new RuntimeException("写入 state.json 失败: " + e.getMessage(), e);
        }
    }

    public MiniClawState updateHeartbeat(long dailyLogBytes, boolean needsDistill) {
        MiniClawState state = load();
        state.setLastHeartbeat(OffsetDateTime.now().toString());
        state.setDailyLogBytes(dailyLogBytes);
        state.setNeedsDistill(needsDistill);
        save(state);
        return state;
    }

    public Path getStatePath() {
        return properties.getHomePath().resolve("state.json");
    }

}

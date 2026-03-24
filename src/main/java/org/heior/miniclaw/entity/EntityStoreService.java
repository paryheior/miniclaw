package org.heior.miniclaw.entity;


import com.fasterxml.jackson.databind.ObjectMapper;
import org.heior.miniclaw.analytics.MiniClawAnalytics;
import org.heior.miniclaw.config.MiniClawProperties;
import org.springframework.stereotype.Service;


import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class EntityStoreService {
    private final MiniClawProperties properties;
    private final ObjectMapper objectMapper;

    public EntityStoreService(MiniClawProperties miniClawProperties,
                              ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.properties = miniClawProperties;
    }

    public Path getEntityStorePath() {
        return properties.getHomePath().resolve("entities.json");
    }

    public EntityStore loadStore() {
        Path path = getEntityStorePath();
        try {
            if (Files.notExists(path)) {
                EntityStore store = new EntityStore();
                saveStore(store);
                return store;
            }
            return objectMapper.readValue(path.toFile(), EntityStore.class);
        } catch (IOException e) {
            throw new RuntimeException("读取 entities.json 失败: " + e.getMessage(), e);
        }

    }

    public void saveStore(EntityStore store) {
        try {
            Files.createDirectories(properties.getHomePath());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(getEntityStorePath().toFile(), store);
        } catch (IOException e) {
            throw new RuntimeException("写入 entities.json 失败: " + e.getMessage(), e);
        }
    }

    public Entity add(String name, String type, Map<String, String> attributes, String sentiment) {
        validateName(name);
        validateType(type);

        EntityStore store = loadStore();
        Entity existing = findByName(store.getEntities(), name);

        String today = LocalDate.now().toString();

        if (existing != null) {
            existing.setLastMentioned(today);
            existing.setMentionCount(existing.getMentionCount() + 1);

            if (attributes != null) {
                existing.getAttributes().putAll(attributes);
            }
            if (sentiment != null && !sentiment.isBlank()) {
                existing.setSentiment(sentiment);
            }

            saveStore(store);
            return existing;
        }

        Entity entity = new Entity();
        entity.setName(name);
        entity.setType(type);
        entity.setFirstMentioned(today);
        entity.setLastMentioned(today);
        entity.setMentionCount(1);
        entity.setAttributes(attributes != null ? new java.util.HashMap<>(attributes) : new java.util.HashMap<>());
        entity.setRelations(new ArrayList<>());
        entity.setSentiment(sentiment);

        store.getEntities().add(entity);
        saveStore(store);
        return entity;
    }
    public Entity query(String name) {
        validateName(name);
        EntityStore store = loadStore();
        return findByName(store.getEntities(), name);
    }

    public List<Entity> list(String filterType) {
        EntityStore store = loadStore();
        if (filterType == null || filterType.isBlank()) {
            return store.getEntities();
        }
        return store.getEntities().stream()
                .filter(entity -> filterType.equalsIgnoreCase(entity.getType()))
                .toList();
    }

    public Entity link(String name, String relation) {
        validateName(name);
        if (relation == null || relation.isBlank()) {
            throw new IllegalArgumentException("relation 不能为空");
        }

        EntityStore store = loadStore();
        Entity entity = findByName(store.getEntities(), name);
        if (entity == null) {
            throw new IllegalArgumentException("实体不存在: " + name);
        }

        if (!entity.getRelations().contains(relation)) {
            entity.getRelations().add(relation);
        }
        entity.setLastMentioned(LocalDate.now().toString());

        saveStore(store);
        return entity;
    }
    private Entity findByName(List<Entity> entities, String name) {
        return entities.stream()
                .filter(entity -> entity.getName() != null && entity.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name 不能为空");
        }
    }

    private void validateType(String type) {
        List<String> allowed = List.of("person", "project", "tool", "concept", "place", "other");
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type 不能为空");
        }
        if (!allowed.contains(type)) {
            throw new IllegalArgumentException("非法 type: " + type);
        }
    }


}

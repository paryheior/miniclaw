package org.heior.miniclaw.entity;

import java.util.ArrayList;
import java.util.List;

public class EntityStore {

    private List<Entity> entities = new ArrayList<>();

    public List<Entity> getEntities() {
        return entities;
    }

    public void setEntities(List<Entity> entities) {
        this.entities = entities;
    }
}
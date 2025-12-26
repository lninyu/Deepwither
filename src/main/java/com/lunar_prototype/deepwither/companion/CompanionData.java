package com.lunar_prototype.deepwither.companion;

import org.bukkit.entity.EntityType;

import java.util.ArrayList;
import java.util.List;

public class CompanionData {
    private final String id;
    private final String name;
    private final String modelId;
    private final EntityType type;
    private final double maxHealth;
    private final boolean rideable;
    private final double speed;
    private final List<CompanionSkill> skills;

    public CompanionData(String id, String name, String modelId, EntityType type, double maxHealth, boolean rideable, double speed) {
        this.id = id;
        this.name = name;
        this.modelId = modelId;
        this.type = type;
        this.maxHealth = maxHealth;
        this.rideable = rideable;
        this.speed = speed;
        this.skills = new ArrayList<>();
    }

    public void addSkill(String skillName, double chance, int cooldown, double range) {
        skills.add(new CompanionSkill(skillName, chance, cooldown, range));
    }

    // Getters...
    public String getId() { return id; }
    public String getName() { return name; }
    public String getModelId() { return modelId; }
    public EntityType getType() { return type; }
    public double getMaxHealth() { return maxHealth; }
    public boolean isRideable() { return rideable; }
    public double getSpeed() { return speed; }
    public List<CompanionSkill> getSkills() { return skills; }

    public static class CompanionSkill {
        String skillName;
        double chance;
        int cooldown;
        double range;

        public CompanionSkill(String skillName, double chance, int cooldown, double range) {
            this.skillName = skillName;
            this.chance = chance;
            this.cooldown = cooldown;
            this.range = range;
        }
    }
}
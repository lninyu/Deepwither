//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lunar_prototype.deepwither.profiler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.util.Vector;

public class CombatProfile {
    public UUID entityUUID;
    public String mobInternalName;
    public long startTime;
    public double initialHealth;
    public UUID lastAttackerUUID;
    public double totalPlayerDamageTaken = (double)0.0F;
    public int playerEquipScore = 0;
    public List<DamageRecord> damageHistory = new ArrayList();
    public Map<String, Integer> playerInputStat = new HashMap();

    public CombatProfile(UUID uuid, String name, double hp) {
        this.entityUUID = uuid;
        this.mobInternalName = name;
        this.startTime = System.currentTimeMillis();
        this.initialHealth = hp;
    }

    public static record DamageRecord(long timestamp, double damage, String cause, double distance, Vector playerInput) {
    }
}

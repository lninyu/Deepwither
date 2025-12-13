package com.lunar_prototype.deepwither.crafting;

import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.SerializableAs;

import java.util.*;

@SerializableAs("CraftingData")
public class CraftingData implements ConfigurationSerializable {

    private final UUID playerId;
    private final List<CraftingJob> jobs;

    public CraftingData(UUID playerId) {
        this.playerId = playerId;
        this.jobs = new ArrayList<>();
    }

    public UUID getPlayerId() { return playerId; }
    public List<CraftingJob> getJobs() { return jobs; }

    public void addJob(CraftingJob job) {
        this.jobs.add(job);
    }

    public void removeJob(UUID jobId) {
        jobs.removeIf(job -> job.getJobId().equals(jobId));
    }

    @Override
    public Map<String, Object> serialize() {
        Map<String, Object> map = new HashMap<>();
        map.put("playerId", playerId.toString());

        List<Map<String, Object>> jobsList = new ArrayList<>();
        for (CraftingJob job : jobs) {
            Map<String, Object> jobMap = new HashMap<>();
            jobMap.put("jobId", job.getJobId().toString());
            jobMap.put("recipeId", job.getRecipeId());
            jobMap.put("resultItemId", job.getResultItemId());
            jobMap.put("completionTime", job.getCompletionTimeMillis());
            jobsList.add(jobMap);
        }
        map.put("jobs", jobsList);
        return map;
    }

    public static CraftingData deserialize(Map<String, Object> map) {
        UUID playerId = UUID.fromString((String) map.get("playerId"));
        CraftingData data = new CraftingData(playerId);

        if (map.containsKey("jobs")) {
            List<Map<String, Object>> jobsList = (List<Map<String, Object>>) map.get("jobs");
            for (Map<String, Object> jobMap : jobsList) {
                data.addJob(new CraftingJob(
                        UUID.fromString((String) jobMap.get("jobId")),
                        (String) jobMap.get("recipeId"),
                        (String) jobMap.get("resultItemId"),
                        (long) jobMap.get("completionTime") // YAMLによってはNumberキャストが必要な場合あり
                ));
            }
        }
        return data;
    }
}
package com.lunar_prototype.deepwither.crafting;

import java.util.UUID;

public class CraftingJob {
    private final UUID jobId;
    private final String recipeId;
    private final String resultItemId;
    private final long completionTimeMillis; // 完了予定時刻 (Unix Time)

    public CraftingJob(String recipeId, String resultItemId, long completionTimeMillis) {
        this.jobId = UUID.randomUUID();
        this.recipeId = recipeId;
        this.resultItemId = resultItemId;
        this.completionTimeMillis = completionTimeMillis;
    }

    // ロード用コンストラクタ
    public CraftingJob(UUID jobId, String recipeId, String resultItemId, long completionTimeMillis) {
        this.jobId = jobId;
        this.recipeId = recipeId;
        this.resultItemId = resultItemId;
        this.completionTimeMillis = completionTimeMillis;
    }

    public UUID getJobId() { return jobId; }
    public String getRecipeId() { return recipeId; }
    public String getResultItemId() { return resultItemId; }
    public long getCompletionTimeMillis() { return completionTimeMillis; }

    public boolean isFinished() {
        return System.currentTimeMillis() >= completionTimeMillis;
    }
}
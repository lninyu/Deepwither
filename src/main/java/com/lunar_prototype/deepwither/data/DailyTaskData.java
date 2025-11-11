package com.lunar_prototype.deepwither.data;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DailyTaskData {

    private final UUID playerId;
    private LocalDate lastResetDate;

    private final Map<String, Integer> completionCounts;

    // Key: Trader ID, Value: [0: Current Kill Count, 1: Target Kill Count]
    private final Map<String, int[]> currentProgress; // PK関連の要素を削除

    public DailyTaskData(UUID playerId) {
        this.playerId = playerId;
        this.lastResetDate = LocalDate.now();
        this.completionCounts = new HashMap<>();
        this.currentProgress = new HashMap<>();
    }

    /// --- リセット関連 ---
    public void checkAndReset() {
        if (!lastResetDate.isEqual(LocalDate.now())) {
            this.completionCounts.clear();
            this.currentProgress.clear();
            this.lastResetDate = LocalDate.now();
            // TODO: プレイヤーにリセットメッセージを送る (またはログイン時に確認)
        }
    }

    // --- 完了回数関連 ---
    public int getCompletionCount(String traderId) {
        checkAndReset();
        return completionCounts.getOrDefault(traderId, 0);
    }

    public void incrementCompletionCount(String traderId) {
        checkAndReset();
        completionCounts.put(traderId, completionCounts.getOrDefault(traderId, 0) + 1);
    }

    // --- 進捗関連 (修正) ---
    public int[] getProgress(String traderId) {
        checkAndReset();
        // [Current Kill Count, Target Kill Count]
        return currentProgress.getOrDefault(traderId, new int[]{0, 0});
    }

    public void setProgress(String traderId, int current, int target) {
        checkAndReset();
        currentProgress.put(traderId, new int[]{current, target});
    }

    public Map<String, int[]> getCurrentProgress() {
        checkAndReset();
        return currentProgress;
    }
}
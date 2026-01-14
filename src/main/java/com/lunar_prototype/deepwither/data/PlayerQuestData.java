package com.lunar_prototype.deepwither.data;

import java.util.*;

/**
 * プレイヤーのクエスト進行状況を保持するクラス
 */
public class PlayerQuestData {
    // [TraderID] -> [完了済みQuestIDのセット]
    private final Map<String, Set<String>> completedQuests = new HashMap<>();

    public Map<String, Integer> getCurrentProgress() {
        return currentProgress;
    }

    private final Map<String, Integer> currentProgress = new HashMap<>();

    public void completeQuest(String traderId, String questId) {
        completedQuests.computeIfAbsent(traderId, k -> new HashSet<>()).add(questId);
    }

    public boolean isCompleted(String traderId, String questId) {
        if (questId == null || questId.isEmpty()) return true;
        Set<String> completed = completedQuests.get(traderId);
        return completed != null && completed.contains(questId);
    }

    public Map<String, Set<String>> getCompletedQuests() {
        return completedQuests;
    }
}
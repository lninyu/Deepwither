package com.lunar_prototype.deepwither;

import com.lunar_prototype.deepwither.data.DailyTaskData;
import org.bukkit.entity.Player;

import java.util.*;

public class DailyTaskManager {

    private final Deepwither plugin;
    // Key: Player UUID, Value: DailyTaskData
    private final Map<UUID, DailyTaskData> playerTaskData;

    public DailyTaskManager(Deepwither plugin) {
        this.plugin = plugin;
        this.playerTaskData = new HashMap<>();
    }

    // --- データ取得 ---
    public DailyTaskData getTaskData(Player player) {
        // データがない場合は新規作成し、マップに追加
        DailyTaskData data = playerTaskData.computeIfAbsent(
                player.getUniqueId(),
                k -> new DailyTaskData(k)
        );
        data.checkAndReset(); // 日付が変わっていたらリセット
        return data;
    }

    // --- タスク開始/リセット ---

    /**
     * 新しいMobキルタスクを発行する
     */
    public void startNewTask(Player player, String traderId) { // isMobTask引数を削除
        DailyTaskData data = getTaskData(player);

        // 目標の数をランダムに設定 (例: バンディットキルなら10-30体)
        int targetCount = plugin.getRandom().nextInt(50) + 20; // 10から30

        // 進捗を設定: [Current Kill Count: 0, Target Kill Count: targetCount]
        data.setProgress(traderId, 0, targetCount);

        player.sendMessage("§e[タスク] §a" + traderId + "§fからの新しいタスクを開始しました！");
        player.sendMessage("§7目標: バンディットを §c" + targetCount + "体 §7倒せ！");
    }

    // --- 進捗更新 ---

    public void updateKillProgress(Player player, String traderId) { // Mob/PKを統合し、単一メソッド化
        DailyTaskData data = getTaskData(player);
        int[] progress = data.getProgress(traderId); // [Current Kill Count, Target Kill Count]

        if (progress[1] > 0) { // タスクを受けている場合
            progress[0]++;
            data.setProgress(traderId, progress[0], progress[1]); // 更新

            if (progress[0] >= progress[1]) {
                //completeTask(player, traderId);
                player.sendMessage("§6§lタスク目標達成！§f トレーダーに報告してください。");
            } else {
                player.sendMessage("§e[進捗] バンディットキル: §a" + progress[0] + "§7/" + progress[1]);
            }
        }
    }

    // --- タスク完了 ---

    public void completeTask(Player player, String traderId) {
        DailyTaskData data = getTaskData(player);

        // 報酬を付与
        int goldReward = 500 + plugin.getRandom().nextInt(500); // 500-1000 G
        int creditReward = 100 + plugin.getRandom().nextInt(50);    // 5-10 信用度

        Deepwither.getEconomy().depositPlayer(player, goldReward);

        // CreditManager/TraderManagerの正しいメソッド呼び出しに置き換えてください
        // ここでは仮に TraderManager を使用
        plugin.getCreditManager().addCredit(player.getUniqueId(), traderId, creditReward);

        player.sendMessage("§6§lタスク完了！§f " + traderId + "のタスクをクリアしました！");
        player.sendMessage("§6報酬: §6" + Deepwither.getEconomy().format(goldReward) + " §fと §b" + creditReward + " §f信用度を獲得！");

        // 完了回数をインクリメントし、進捗をリセット
        data.incrementCompletionCount(traderId);
        data.setProgress(traderId, 0, 0);
    }

    public Set<String> getActiveTaskTraders(Player player) {
        DailyTaskData data = getTaskData(player);

        // 進捗データcurrentProgressのキーセットを取得
        Set<String> activeTraders = new HashSet<>();
        for (Map.Entry<String, int[]> entry : data.getCurrentProgress().entrySet()) {
            // progress[1] > 0 は、タスク目標値が設定されていることを意味する
            if (entry.getValue()[1] > 0) {
                activeTraders.add(entry.getKey());
            }
        }
        return activeTraders;
    }

    // TODO: データセーブ/ロード処理
    // ...
}
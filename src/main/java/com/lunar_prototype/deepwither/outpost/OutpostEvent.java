package com.lunar_prototype.deepwither.outpost;

import com.lunar_prototype.deepwither.Deepwither;
import org.bukkit.Bukkit;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class OutpostEvent {

    private final JavaPlugin plugin;
    private final OutpostManager manager;
    private final OutpostConfig.OutpostData outpostData;
    private final ContributionTracker tracker;
    private final Set<UUID> participants = new HashSet<>();
    private int currentWave = 0;
    private int totalMobs = 0; // 現在アクティブなOutpostモブの総数
    private int waveTaskId = -1; // ウェーブタイマーのID

    // ★追加: イベント開始を待機するタスクID
    private int initialWaitTaskId = -1;
    // ★追加: 初回ウェーブ開始までの待機時間（秒）
    private static final int INITIAL_WAIT_SECONDS = 120; // 例: 120秒待機

    public OutpostEvent(JavaPlugin plugin, OutpostManager manager, OutpostConfig.OutpostData data, OutpostConfig.ScoreWeights weights) {
        this.plugin = plugin;
        this.manager = manager;
        this.outpostData = data;
        this.tracker = new ContributionTracker(weights);
        // MobSpawnManager連携のためにWorldGuard Region IDを渡す
        Deepwither.getInstance().getMobSpawnManager().disableNormalSpawning(data.getRegionName());
    }

    public void startEvent() {
        // イベント開始通知
        Bukkit.broadcastMessage("§e§l【Outpost発生】§f " + outpostData.getDisplayName() + " にPvEイベントが発生しました！");
        startInitialWaitTimer();
    }

    /**
     * 初回参加者がいない場合にイベントをキャンセルする待機タイマーを開始します。
     */
    private void startInitialWaitTimer() {
        // 待機時間 (例: 2分) をプレイヤーに通知
        Bukkit.broadcastMessage("§7[Outpost] " + outpostData.getDisplayName() + " は " + INITIAL_WAIT_SECONDS + "秒以内に参加者がいない場合、キャンセルされます。");

        this.initialWaitTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // 待機時間が終了した時点で参加者が0人だった場合
            if (participants.isEmpty()) {
                Bukkit.broadcastMessage("§c【Outpost】参加者がいなかったため、イベント「" + outpostData.getDisplayName() + "」はキャンセルされました。");
                // MobSpawnManagerの無効化を解除し、Managerに終了を通知
                Deepwither.getInstance().getMobSpawnManager().enableNormalSpawning(outpostData.getRegionName());
                manager.endActiveEvent();
            } else {
                // 時間切れになったが参加者がいた場合、そのままウェーブを開始する
                Bukkit.broadcastMessage("§a【Outpost】待機時間終了！ウェーブを開始します。");
                runNextWave();
            }
            this.initialWaitTaskId = -1; // タスクIDをリセット
        }, INITIAL_WAIT_SECONDS * 20L).getTaskId();
    }

    private void runNextWave() {
        currentWave++;
        OutpostConfig.WaveData waveData = outpostData.getWaves().get(currentWave);

        if (waveData == null) {
            // 全ウェーブ完了
            finishEvent();
            return;
        }

        // Mobスポーン
        totalMobs = spawnWaveMobs(waveData.getMobList());

        sendParticipantMessage("§6§l--- ウェーブ " + currentWave + " / " + outpostData.getWaves().size() + " 開始 ---"); // ウェーブ総数を表示

        // ウェーブタイマー設定
        long durationTicks = waveData.getDurationSeconds() * 20L;
        waveTaskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            // managerフィールドを使ってOutpostManagerにアクセスし、
            // アクティブなイベントが自分自身（this）であり、ウェーブ番号が一致するかを確認する
            if (manager.getActiveEvent() == this && this.currentWave == currentWave) {
                // ★修正: Mobが残っていても強制終了せず、次のウェーブへ強制移行する
                forceNextWave();
            }
        }, durationTicks).getTaskId();
    }

    /**
     * ウェーブ時間切れ時や強制移行時に残存Mobを処理し、次のウェーブに進みます。
     */
    private void forceNextWave() {
        Bukkit.getLogger().info("[OutpostEvent] ウェーブ " + currentWave + " が時間切れになりました。残存Mobを強制処理します。");

        // MobSpawnManagerと連携し、現在アクティブなOutpost Mobを全て削除する
        // MobSpawnManagerに、このイベントのリージョンIDに紐づいたMobを削除するメソッドが必要
        Deepwither.getInstance().getMobSpawnManager().removeAllOutpostMobs(outpostData.getRegionName());

        // Mob数をリセット
        totalMobs = 0;

        sendParticipantMessage("§cウェーブ時間切れ！残存Mobを処理し、次のウェーブへ移行します。");

        // 次のウェーブを開始
        runNextWave();
    }

    /**
     * プレイヤーを参加者リストに追加します。
     * @param playerUUID 参加プレイヤーのUUID
     */
    public void addParticipant(UUID playerUUID) {
        // 既存の参加者でなければ追加
        if (participants.add(playerUUID)) {
            // ★修正: 初回参加者チェック
            if (participants.size() == 1) {
                // 最初の参加者が出現したら、待機タイマーを強制的にキャンセルし、ウェーブを開始
                if (initialWaitTaskId != -1) {
                    Bukkit.getScheduler().cancelTask(initialWaitTaskId);
                    initialWaitTaskId = -1;

                    Bukkit.broadcastMessage("§a§l【Outpost】最初の参加者が出現したため、即座にウェーブを開始します！");
                    runNextWave();
                }
            }
        }
    }

    /**
     * 参加者リスト（UUIDのセット）を返します。
     * @return 参加者UUIDの不変Set
     */
    public Set<UUID> getParticipants() {
        // 外部からの変更を防ぐため、不変なビューを返すのがベストプラクティス
        return Collections.unmodifiableSet(participants);
    }

    public OutpostConfig.OutpostData getOutpostData() {
        return outpostData;
    }

    /**
     * 参加者リストにいるプレイヤーにメッセージを送信します。
     * @param message 送信するメッセージ
     */
    public void sendParticipantMessage(String message) {
        participants.stream()
                .map(Bukkit::getPlayer)
                .filter(player -> player != null && player.isOnline())
                .forEach(player -> player.sendMessage(message));
    }

    /**
     * MobSpawnManagerを使用してMobをスポーンさせます。
     * @return スポーンさせたMobの総数
     */
    private int spawnWaveMobs(Map<String, Integer> mobList) {
        int count = 0;
        double fixedY = outpostData.getSpawnYCoordinate();
        // MobSpawnManagerにスポーン処理を依頼 (OutpostEventの参照とRegionIDを渡す)
        for (Map.Entry<String, Integer> entry : mobList.entrySet()) {
            count += Deepwither.getInstance().getMobSpawnManager().spawnOutpostMobs(
                    entry.getKey(),
                    entry.getValue(),
                    outpostData.getRegionName(),
                    fixedY,
                    this // イベントの参照を渡してMobの追跡に使う
            );
        }
        return count;
    }

    /**
     * Mob撃破時にMobSpawnManagerから呼ばれることを想定。
     */
    public void mobDefeated(Entity mob, UUID killer) {
        if (totalMobs > 0) {
            totalMobs--;
        }

        Deepwither.getInstance().getMobSpawnManager().untrackOutpostMob(mob.getUniqueId());

        // キル数をContributionTrackerに記録
        tracker.addKill(killer);

        if (totalMobs <= 0) {
            // Mobが全て倒された場合、タイマーをキャンセルして次のウェーブへ
            if (waveTaskId != -1) {
                Bukkit.getScheduler().cancelTask(waveTaskId);
            }
            runNextWave();
        }
    }

    private void finishEvent() {
        // MobSpawnManagerに通常スポーンの許可を戻す
        Deepwither.getInstance().getMobSpawnManager().enableNormalSpawning(outpostData.getRegionName());

        // イベント終了をManagerに通知
        manager.endActiveEvent();
    }

    /**
     * 報酬を配布し、貢献度を参加者に表示します。
     */
    public void distributeRewards() {
        // 貢献度ランキングを計算し、降順リストを取得
        List<Map.Entry<UUID, Double>> rankings = tracker.getRankings();
        if (rankings.isEmpty()) {
            Bukkit.getLogger().info("[Outpost] イベント終了: 報酬配布対象の参加者がいませんでした。");
            return;
        }

        // 貢献度上位3名のUUIDを抽出
        List<UUID> top3 = rankings.stream()
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        // 報酬配布ロジックのための計算
        double totalScore = rankings.stream().mapToDouble(Map.Entry::getValue).sum();
        double averageScore = totalScore / rankings.size();

        // 全員にランキング結果を通知
        sendParticipantMessage("§a§l--- Outpost イベント終了: 貢献度ランキング ---");

        // ランキング表示メッセージを作成
        for (int i = 0; i < rankings.size(); i++) {
            Map.Entry<UUID, Double> entry = rankings.get(i);
            Player p = Bukkit.getPlayer(entry.getKey());
            String playerName = (p != null) ? p.getName() : entry.getKey().toString().substring(0, 8);

            String rankPrefix = "§f" + (i + 1) + ". ";
            if (i == 0) rankPrefix = "§e§l1. ";
            if (i == 1) rankPrefix = "§e2. ";
            if (i == 2) rankPrefix = "§63. ";

            String rankMessage = String.format("%s%s (§b%.2f§f スコア)", rankPrefix, playerName, entry.getValue());
            sendParticipantMessage(rankMessage);
        }
        sendParticipantMessage("§a--------------------------------------");

        // 全参加者に貢献度と報酬を配布
        for (int i = 0; i < rankings.size(); i++) {
            Map.Entry<UUID, Double> entry = rankings.get(i);
            UUID playerUUID = entry.getKey();
            double score = entry.getValue();
            int rank = i + 1;

            Player p = Bukkit.getPlayer(playerUUID);
            if (p == null) {
                Bukkit.getLogger().warning("[Outpost Debug] プレイヤー " + playerUUID + " はオフラインのため、報酬を配布できませんでした。");
                continue; // オフラインの場合は次のプレイヤーへ
            }

            // プレイヤー個人への通知
            p.sendMessage("§b【貢献度】§f あなたの順位は §e§l" + rank + "位§f、スコアは §b" + String.format("%.2f", score) + "§f でした。");


            // 報酬の選択
            List<OutpostConfig.RewardItem> rewards;
            String rewardTier;

            if (rank <= 3) {
                rewards = outpostData.getRewards().getTopContributor();
                rewardTier = "TOP3";
            } else if (score >= averageScore * 0.5) {
                rewards = outpostData.getRewards().getAverageContributor();
                rewardTier = "AVERAGE";
            } else {
                rewards = outpostData.getRewards().getMinimumReward();
                rewardTier = "MINIMUM";
            }

            Bukkit.getLogger().info("[Outpost Debug] " + p.getName() + "(スコア: " + String.format("%.2f", score) + ") に " + rewardTier + " 報酬を配布開始 (報酬数: " + rewards.size() + ")");

            // 報酬の配布実行 (ItemFactoryを使用)
            if (rewards.isEmpty()) {
                p.sendMessage("§7[Outpost] §f報酬ティア: " + rewardTier + " に設定されたアイテムはありませんでした。");
                Bukkit.getLogger().warning("[Outpost Debug] " + p.getName() + " の " + rewardTier + " 報酬リストは空です。設定を確認してください。");
                return; // この報酬ティアの配布をスキップして次のプレイヤーへ
            }

            rewards.forEach(reward -> {
                int min = reward.getMinQuantity();
                int max = reward.getMaxQuantity();

                // 数量をランダムに決定
                int quantity = Deepwither.getInstance().getRandom().nextInt(max - min + 1) + min;

                // カスタムアイテムの生成
                ItemStack customItem = Deepwither.getInstance().itemFactory.getCustomCountItemStack(reward.getCustomItemId(), quantity);

                if (customItem != null) {
                    p.getInventory().addItem(customItem);
                    p.sendMessage("§a[Outpost] §f報酬アイテム: §e" + customItem.getItemMeta().getDisplayName() + " §fx" + quantity);
                    Bukkit.getLogger().info("[Outpost Debug] -> 報酬配布成功: " + p.getName() + " に " + quantity + " x " + reward.getCustomItemId());
                } else {
                    p.sendMessage("§c[Outpost] §4報酬アイテム生成エラー: ID " + reward.getCustomItemId());
                    Bukkit.getLogger().warning("[Outpost Debug] -> 報酬配布失敗: " + p.getName() + " のアイテムID " + reward.getCustomItemId() + " が無効です。");
                }
            });
        }
    }

    // イベントリスナーからTrackerにデータを渡すためのゲッター
    public ContributionTracker getTracker() {
        return tracker;
    }

    // MobSpawnManagerがMobにタグ付けするために使用するID
    public String getOutpostRegionId() {
        return outpostData.getRegionName();
    }
}
package com.lunar_prototype.deepwither.outpost;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import java.util.concurrent.TimeUnit;
import java.util.Random;

public class OutpostManager {

    private static OutpostManager instance;
    private final JavaPlugin plugin;
    private final OutpostConfig config;
    private OutpostEvent activeEvent = null;
    private long cooldownEnds = 0;
    private final Random random = new Random();

    private OutpostManager(JavaPlugin plugin, OutpostConfig config) {
        this.plugin = plugin;
        this.config = config;
    }

    public static OutpostManager initialize(JavaPlugin plugin, OutpostConfig config) {
        if (instance == null) {
            instance = new OutpostManager(plugin, config);
            instance.startScheduler();
        }
        return instance;
    }

    public static OutpostManager getInstance() {
        return instance;
    }

    /**
     * イベント抽選用のスケジューラーを開始します。
     */
    public void startScheduler() {
        long intervalTicks = config.getGlobalSettings().getCheckIntervalMinutes() * 60 * 20L;

        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::checkAndStartEvent, intervalTicks, intervalTicks);
    }

    /**
     * イベント開始の抽選と条件チェックを行います。
     */
    private void checkAndStartEvent() {
        if (activeEvent != null || isCooldownActive()) {
            return;
        }

        if (Bukkit.getOnlinePlayers().size() < config.getGlobalSettings().getMinPlayersOnline()) {
            return;
        }

        // 確率抽選
        if (random.nextInt(100) < config.getGlobalSettings().getEventChancePercent()) {
            startRandomOutpost();
        }
    }

    /**
     * ランダムなOutpostを選択し、イベントを開始します。
     */
    public void startRandomOutpost() {
        // 利用可能なOutpostをリスト化
        OutpostConfig.OutpostData[] available = config.getOutposts().values().toArray(new OutpostConfig.OutpostData[0]);
        if (available.length == 0) return;

        // ランダムに一つ選択
        OutpostConfig.OutpostData selectedOutpost = available[random.nextInt(available.length)];

        // イベントの開始
        activeEvent = new OutpostEvent(plugin, this, selectedOutpost, config.getWeights());
        activeEvent.startEvent();

        plugin.getLogger().info("Outpostイベントが開始されました: " + selectedOutpost.getDisplayName());
        // プレイヤーへの通知処理などをここに追加
    }

    /**
     * アクティブなイベントを終了し、報酬を配布し、クールダウンを設定します。
     */
    public void endActiveEvent() {
        if (activeEvent == null) return;

        // 1. 報酬配布ロジックを実行
        activeEvent.distributeRewards();

        // 2. クールダウンを設定
        long cooldownMinutes = config.getGlobalSettings().getCooldownMinutes();
        cooldownEnds = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(cooldownMinutes);

        plugin.getLogger().info("Outpostイベントが終了しました。クールダウン: " + cooldownMinutes + "分");

        // 3. イベントをリセット
        activeEvent = null;
    }

    public boolean isCooldownActive() {
        return cooldownEnds > System.currentTimeMillis();
    }

    public OutpostEvent getActiveEvent() {
        return activeEvent;
    }
}
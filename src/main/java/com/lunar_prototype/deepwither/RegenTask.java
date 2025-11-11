package com.lunar_prototype.deepwither;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RegenTask extends BukkitRunnable {

    private final StatManager statManager;
    // タスクが実行される間隔（秒）
    private static final double INTERVAL_SECONDS = 2.0;

    public RegenTask(StatManager statManager) {
        this.statManager = statManager;
    }

    @Override
    public void run() {
        for (Player player : Deepwither.getInstance().getServer().getOnlinePlayers()) {

            // 死亡していないかチェック (HPが0.0でなければ回復)
            if (statManager.getActualCurrentHealth(player) > 0.0) {
                statManager.naturalRegeneration(player, INTERVAL_SECONDS);
            }
        }
    }

    /**
     * DeepWitherプラグインでこのタスクを開始するためのヘルパー。
     * @param plugin DeepWitherインスタンス
     */
    public void start(Deepwither plugin) {
        // 2秒 (40ティック) ごとに実行
        this.runTaskTimer(plugin, 0L, (long) (INTERVAL_SECONDS * 20));
    }
}
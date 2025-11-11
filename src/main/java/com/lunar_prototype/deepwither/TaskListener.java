package com.lunar_prototype.deepwither;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

public class TaskListener implements Listener {

    private final DailyTaskManager taskManager;
    private static final String DEFAULT_TASK_TRADER = "DailyTask"; // 共有タスクID

    public TaskListener(DailyTaskManager taskManager) {
        this.taskManager = taskManager;
    }

    // A. Mobキルタスクの進捗
    @EventHandler(priority = EventPriority.MONITOR)
    public void onEntityDeath(EntityDeathEvent e) {
        if (e.getEntityType() == EntityType.PLAYER) return; // プレイヤーの死亡は除外

        Player killer = e.getEntity().getKiller();
        if (killer == null) return;

        // MOBの死亡はMobキルタスクとして扱う
        taskManager.updateKillProgress(killer, DEFAULT_TASK_TRADER);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMythicMobDeath(MythicMobDeathEvent e) {
        Player killer = e.getKiller() instanceof Player ? (Player) e.getKiller() : null;
        if (killer == null) return;

        // ★ バンディットIDのチェック
        String mobId = e.getMobType().getInternalName();
        if (mobId.toLowerCase().contains("bandit")) {

            // ★ 修正: 進捗中の全てのトレーダーIDを取得し、それぞれ更新を試みる
            for (String traderId : taskManager.getActiveTaskTraders(killer)) {
                taskManager.updateKillProgress(killer, traderId);
            }
        }
    }
}
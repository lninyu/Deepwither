package com.lunar_prototype.deepwither;

import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

public class MobKillListener implements Listener {
    private final LevelManager levelManager;
    private final FileConfiguration mobExpConfig;

    public MobKillListener(LevelManager levelManager, FileConfiguration config) {
        this.levelManager = levelManager;
        this.mobExpConfig = config;
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent e) {
        if (!(e.getKiller() instanceof Player player)) return;

        String mobType = e.getMobType().getInternalName();
        double exp = mobExpConfig.getDouble("mob-exp." + mobType, 0);
        levelManager.updatePlayerDisplay(player);

        if (exp > 0) {
            levelManager.addExp(player, exp);
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();

        // 1. バニラの経験値加算をキャンセル
        // これにより、プレイヤーの内部レベルは上がりません。
        event.setAmount(0);

        // カスタム経験値を加算するメソッドを呼び出す
        levelManager.updatePlayerDisplay(player);
    }
}


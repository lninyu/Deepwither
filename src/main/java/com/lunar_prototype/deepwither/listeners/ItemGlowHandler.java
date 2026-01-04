package com.lunar_prototype.deepwither.listeners;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.scoreboard.Team;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ItemGlowHandler implements Listener {

    // 1. レアリティ名と ChatColor を直接紐付けるマップに変更
    private static final Map<String, ChatColor> RARITY_CONFIG = Map.of(
            "コモン", ChatColor.WHITE,
            "アンコモン", ChatColor.GREEN,
            "レア", ChatColor.AQUA,
            "エピック", ChatColor.LIGHT_PURPLE,
            "レジェンダリー", ChatColor.GOLD
    );

    private static final String TEAM_PREFIX = "dw_glow_";
    private final Scoreboard scoreboard;

    public ItemGlowHandler(JavaPlugin plugin) {
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        initializeTeams(plugin);
    }

    private void initializeTeams(JavaPlugin plugin) {
        // RARITY_CONFIG に定義された色ごとにチームを作成
        for (ChatColor color : RARITY_CONFIG.values()) {
            String teamName = TEAM_PREFIX + color.name();
            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }
            team.setColor(color);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemSpawn(ItemSpawnEvent event) {
        Item itemEntity = event.getEntity();
        ItemStack itemStack = itemEntity.getItemStack();

        if (!itemStack.hasItemMeta()) return;
        ItemMeta meta = itemStack.getItemMeta();
        if (!meta.hasLore()) return;

        List<String> lore = meta.getLore();
        ChatColor targetColor = null;

        for (String line : lore) {
            // 色コードを剥がして比較（§7レアリティ:§f§6... を レアリティ:... に変換）
            String cleanLine = ChatColor.stripColor(line);

            if (cleanLine.contains("レアリティ")) {
                // RARITY_CONFIG のキー（コモン、レア等）が含まれているかループで確認
                for (Map.Entry<String, ChatColor> entry : RARITY_CONFIG.entrySet()) {
                    if (cleanLine.contains(entry.getKey())) {
                        targetColor = entry.getValue();
                        break;
                    }
                }
            }
            if (targetColor != null) break;
        }

        if (targetColor != null) {
            itemEntity.setGlowing(true);
            String teamName = TEAM_PREFIX + targetColor.name();
            Team team = scoreboard.getTeam(teamName);

            if (team != null) {
                team.addEntry(itemEntity.getUniqueId().toString());
            }
        }
    }
}

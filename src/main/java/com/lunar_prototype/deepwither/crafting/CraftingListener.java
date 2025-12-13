package com.lunar_prototype.deepwither.crafting;

import com.lunar_prototype.deepwither.Deepwither;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.UUID;

public class CraftingListener implements Listener {

    private final Deepwither plugin;

    public CraftingListener(Deepwither plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        String title = e.getView().getTitle();
        if (!title.equals(CraftingGUI.TITLE_RECIPES) && !title.equals(CraftingGUI.TITLE_QUEUE)) {
            return;
        }

        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (e.getCurrentItem() == null || e.getCurrentItem().getType() == Material.AIR) return;

        ItemStack clicked = e.getCurrentItem();

        // --- ナビゲーション処理 ---
        if (clicked.getType() == Material.CHEST && title.equals(CraftingGUI.TITLE_RECIPES)) {
            plugin.getCraftingGUI().openQueueList(player);
            return;
        }
        if (clicked.getType() == Material.CRAFTING_TABLE && title.equals(CraftingGUI.TITLE_QUEUE)) {
            plugin.getCraftingGUI().openRecipeList(player);
            return;
        }

        // --- レシピ選択画面 ---
        if (title.equals(CraftingGUI.TITLE_RECIPES)) {
            NamespacedKey key = new NamespacedKey(plugin, "gui_recipe_id");
            if (clicked.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String recipeId = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);

                if (plugin.getCraftingManager().startCrafting(player, recipeId)) {
                    // 成功したらキュー画面へ遷移させる、または音を鳴らす
                    player.playSound(player.getLocation(), org.bukkit.Sound.BLOCK_ANVIL_USE, 1f, 1f);
                } else {
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                }
            }
        }

        // --- キュー画面（受け取り） ---
        if (title.equals(CraftingGUI.TITLE_QUEUE)) {
            NamespacedKey key = new NamespacedKey(plugin, "gui_job_id");
            if (clicked.getItemMeta().getPersistentDataContainer().has(key, PersistentDataType.STRING)) {
                String uuidStr = clicked.getItemMeta().getPersistentDataContainer().get(key, PersistentDataType.STRING);
                UUID jobId = UUID.fromString(uuidStr);

                plugin.getCraftingManager().claimJob(player, jobId);
                // 画面更新
                plugin.getCraftingGUI().openQueueList(player);
            }
        }
    }
}
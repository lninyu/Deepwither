package com.lunar_prototype.deepwither.crafting;

import com.lunar_prototype.deepwither.Deepwither;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class CraftingGUI {

    public static final String TITLE_RECIPES = ChatColor.DARK_GRAY + "クラフト - レシピ選択";
    public static final String TITLE_QUEUE = ChatColor.DARK_GRAY + "クラフト - 進行状況";

    private final Deepwither plugin;
    private final NamespacedKey recipeKey;
    private final NamespacedKey jobKey;

    public CraftingGUI(Deepwither plugin) {
        this.plugin = plugin;
        this.recipeKey = new NamespacedKey(plugin, "gui_recipe_id");
        this.jobKey = new NamespacedKey(plugin, "gui_job_id");
    }

    // レシピ一覧を開く
    public void openRecipeList(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, TITLE_RECIPES);
        CraftingManager manager = plugin.getCraftingManager();

        int slot = 0;
        File itemFolder = new File(plugin.getDataFolder(), "items");

        for (CraftingRecipe recipe : manager.getAllRecipes()) {
            if (slot >= 45) break; // ページングが必要ならここで処理

            // 完成品のアイコンを表示
            ItemStack displayIcon = Deepwither.getInstance().getItemFactory().getCustomItemStack(recipe.getResultItemId());
            if (displayIcon == null) displayIcon = new ItemStack(Material.BARRIER);

            ItemMeta meta = displayIcon.getItemMeta();
            List<String> lore = meta.getLore() == null ? new ArrayList<>() : meta.getLore();

            lore.add("");
            lore.add(ChatColor.GOLD + "--- 必要素材 ---");
            recipe.getIngredients().forEach((id, amount) -> {
                // カスタムIDをそのまま表示 (必要なら表示名変換を入れる)
                lore.add(ChatColor.GRAY + "- " + Deepwither.getInstance().getItemNameResolver().resolveItemDisplayName(id) + ": " + ChatColor.WHITE + "x" + amount);
            });
            lore.add("");
            lore.add(ChatColor.YELLOW + "製作時間: " + recipe.getTimeSeconds() + "秒");
            lore.add(ChatColor.GREEN + "クリックして製作開始");

            meta.setLore(lore);
            // レシピIDをPDCに埋め込む
            meta.getPersistentDataContainer().set(recipeKey, PersistentDataType.STRING, recipe.getId());
            displayIcon.setItemMeta(meta);

            gui.setItem(slot++, displayIcon);
        }

        // 下部にメニュー切り替えボタン
        addNavigationButtons(gui, true);

        player.openInventory(gui);
    }

    // 進行状況を開く
    public void openQueueList(Player player) {
        Inventory gui = Bukkit.createInventory(null, 54, TITLE_QUEUE);
        CraftingManager manager = plugin.getCraftingManager();
        CraftingData data = manager.getData(player);
        File itemFolder = new File(plugin.getDataFolder(), "items");

        int slot = 0;
        for (CraftingJob job : data.getJobs()) {
            ItemStack icon = Deepwither.getInstance().getItemFactory().getCustomItemStack(job.getResultItemId());
            if (icon == null) icon = new ItemStack(Material.PAPER);

            ItemMeta meta = icon.getItemMeta();
            List<String> lore = new ArrayList<>();

            if (job.isFinished()) {
                meta.setDisplayName(ChatColor.GREEN + "【完成】" + (meta.hasDisplayName() ? meta.getDisplayName() : job.getResultItemId()));
                lore.add(ChatColor.YELLOW + "クリックして受け取る");
                // エンチャントエフェクト等をつけると分かりやすい
            } else {
                long remainingMillis = job.getCompletionTimeMillis() - System.currentTimeMillis();
                long remainingSec = remainingMillis / 1000;
                meta.setDisplayName(ChatColor.YELLOW + "【製作中】" + (meta.hasDisplayName() ? meta.getDisplayName() : job.getResultItemId()));
                lore.add(ChatColor.GRAY + "残り時間: " + remainingSec + "秒");
            }

            meta.setLore(lore);
            // JobIDを埋め込む
            meta.getPersistentDataContainer().set(jobKey, PersistentDataType.STRING, job.getJobId().toString());
            icon.setItemMeta(meta);

            gui.setItem(slot++, icon);
        }

        // 下部にメニュー切り替えボタン
        addNavigationButtons(gui, false);

        player.openInventory(gui);
    }

    private void addNavigationButtons(Inventory gui, boolean isRecipeView) {
        ItemStack glass = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta gMeta = glass.getItemMeta();
        gMeta.setDisplayName(" ");
        glass.setItemMeta(gMeta);

        // 45-53はメニューエリア
        for (int i = 45; i < 54; i++) {
            gui.setItem(i, glass);
        }

        if (isRecipeView) {
            ItemStack queueBtn = new ItemStack(Material.CHEST);
            ItemMeta meta = queueBtn.getItemMeta();
            meta.setDisplayName(ChatColor.AQUA + ">> 進行状況・受取 <<");
            queueBtn.setItemMeta(meta);
            gui.setItem(53, queueBtn);
        } else {
            ItemStack recipeBtn = new ItemStack(Material.CRAFTING_TABLE);
            ItemMeta meta = recipeBtn.getItemMeta();
            meta.setDisplayName(ChatColor.GREEN + "<< レシピ一覧へ戻る");
            recipeBtn.setItemMeta(meta);
            gui.setItem(45, recipeBtn);
        }
    }
}
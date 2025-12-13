package com.lunar_prototype.deepwither.crafting;

import com.lunar_prototype.deepwither.Deepwither;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class CraftingManager {

    private final Deepwither plugin;
    private final CraftingDataStore dataStore;
    private final Map<String, CraftingRecipe> recipes = new HashMap<>();
    private final Map<UUID, CraftingData> sessionCache = new ConcurrentHashMap<>();

    private final NamespacedKey customIdKey;

    public CraftingManager(Deepwither plugin) {
        this.plugin = plugin;
        this.dataStore = new CraftingDataStore(plugin);
        this.customIdKey = new NamespacedKey(plugin, "custom_id"); // 指定されたキー
        loadRecipes();
    }

    public void loadRecipes() {
        recipes.clear();
        File file = new File(plugin.getDataFolder(), "crafting.yml");
        if (!file.exists()) plugin.saveResource("crafting.yml", false);

        YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection section = config.getConfigurationSection("recipes");
        if (section == null) return;

        for (String key : section.getKeys(false)) {
            String result = section.getString(key + ".result_id");
            int time = section.getInt(key + ".time_seconds");

            Map<String, Integer> ingredients = new HashMap<>();
            ConfigurationSection ingSection = section.getConfigurationSection(key + ".ingredients");
            if (ingSection != null) {
                for (String matKey : ingSection.getKeys(false)) {
                    ingredients.put(matKey, ingSection.getInt(matKey));
                }
            }

            recipes.put(key, new CraftingRecipe(key, result, time, ingredients));
        }
        plugin.getLogger().info("Loaded " + recipes.size() + " crafting recipes.");
    }

    // --- プレイヤーデータ管理 ---
    public void loadPlayer(Player player) {
        dataStore.loadData(player.getUniqueId()).thenAccept(data ->
                sessionCache.put(player.getUniqueId(), data)
        );
    }

    public void saveAndUnloadPlayer(UUID playerId) {
        CraftingData data = sessionCache.remove(playerId);
        if (data != null) dataStore.saveData(data);
    }

    public CraftingData getData(Player player) {
        return sessionCache.getOrDefault(player.getUniqueId(), new CraftingData(player.getUniqueId()));
    }

    public Collection<CraftingRecipe> getAllRecipes() {
        return recipes.values();
    }

    // --- クラフト処理 ---
    public boolean startCrafting(Player player, String recipeId) {
        CraftingRecipe recipe = recipes.get(recipeId);
        if (recipe == null) return false;

        // 1. 素材チェック
        if (!hasIngredients(player, recipe.getIngredients())) {
            player.sendMessage(ChatColor.RED + "素材または設計図が不足しています。");
            return false;
        }

        // 2. 素材消費
        consumeIngredients(player, recipe.getIngredients());

        // 3. ジョブ追加
        CraftingData data = getData(player);
        long finishTime = System.currentTimeMillis() + (recipe.getTimeSeconds() * 1000L);
        data.addJob(new CraftingJob(recipe.getId(), recipe.getResultItemId(), finishTime));

        // 4. 保存
        dataStore.saveData(data);

        player.sendMessage(ChatColor.GREEN + "製作を開始しました！完了まで: " + recipe.getTimeSeconds() + "秒");
        return true;
    }

    public void claimJob(Player player, UUID jobId) {
        CraftingData data = getData(player);
        Optional<CraftingJob> jobOpt = data.getJobs().stream().filter(j -> j.getJobId().equals(jobId)).findFirst();

        if (jobOpt.isEmpty()) return;
        CraftingJob job = jobOpt.get();

        if (!job.isFinished()) {
            player.sendMessage(ChatColor.YELLOW + "まだ完成していません。");
            return;
        }

        if (player.getInventory().firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "インベントリがいっぱいです。");
            return;
        }

        // アイテム生成 (ItemLoaderを使用)
        File itemFolder = new File(plugin.getDataFolder(), "items");
        ItemStack resultItem = Deepwither.getInstance().getItemFactory().getCustomItemStack(job.getResultItemId());

        if (resultItem == null) {
            player.sendMessage(ChatColor.RED + "アイテムデータの生成に失敗しました (ID: " + job.getResultItemId() + ")");
            return;
        }

        player.getInventory().addItem(resultItem);
        data.removeJob(jobId);
        dataStore.saveData(data);

        player.sendMessage(ChatColor.GOLD + "アイテムを受け取りました！");
    }

    // --- インベントリ操作ヘルパー ---

    // カスタムIDを取得する
    private String getCustomId(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return null;
        return item.getItemMeta().getPersistentDataContainer().get(customIdKey, PersistentDataType.STRING);
    }

    private boolean hasIngredients(Player player, Map<String, Integer> required) {
        Map<String, Integer> currentCounts = new HashMap<>();

        for (ItemStack item : player.getInventory().getContents()) {
            String cid = getCustomId(item);
            if (cid != null) {
                currentCounts.put(cid, currentCounts.getOrDefault(cid, 0) + item.getAmount());
            }
        }

        for (Map.Entry<String, Integer> entry : required.entrySet()) {
            if (currentCounts.getOrDefault(entry.getKey(), 0) < entry.getValue()) {
                return false;
            }
        }
        return true;
    }

    private void consumeIngredients(Player player, Map<String, Integer> required) {
        // hasIngredientsでチェック済み前提
        Map<String, Integer> toRemove = new HashMap<>(required);

        for (ItemStack item : player.getInventory().getContents()) {
            if (toRemove.isEmpty()) break;

            String cid = getCustomId(item);
            if (cid != null && toRemove.containsKey(cid)) {
                int needed = toRemove.get(cid);
                int amount = item.getAmount();

                if (amount <= needed) {
                    item.setAmount(0); // 全部消費
                    toRemove.put(cid, needed - amount);
                    if (toRemove.get(cid) <= 0) toRemove.remove(cid);
                } else {
                    item.setAmount(amount - needed); // 一部消費
                    toRemove.remove(cid);
                }
            }
        }
    }
}
package com.lunar_prototype.deepwither.crafting;

import com.lunar_prototype.deepwither.Deepwither;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerialization;

import java.io.File;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class CraftingDataStore {
    private final Deepwither plugin;
    private final File dataFolder;

    static {
        ConfigurationSerialization.registerClass(CraftingData.class);
    }

    public CraftingDataStore(Deepwither plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "crafting_data");
        if (!dataFolder.exists()) dataFolder.mkdirs();
    }

    private File getPlayerFile(UUID playerId) {
        return new File(dataFolder, playerId.toString() + ".yml");
    }

    public CompletableFuture<CraftingData> loadData(UUID playerId) {
        return CompletableFuture.supplyAsync(() -> {
            File file = getPlayerFile(playerId);
            if (!file.exists()) return new CraftingData(playerId);

            YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
            return (CraftingData) config.get("data", new CraftingData(playerId));
        });
    }

    public void saveData(CraftingData data) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            File file = getPlayerFile(data.getPlayerId());
            YamlConfiguration config = new YamlConfiguration();
            config.set("data", data);
            try {
                config.save(file);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
package com.lunar_prototype.deepwither.crafting;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.lunar_prototype.deepwither.Deepwither;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PlayerRecipeJsonStore {
    private final Deepwither plugin;
    private final File dataFolder;
    private final Gson gson;

    public PlayerRecipeJsonStore(Deepwither plugin) {
        this.plugin = plugin;
        this.dataFolder = new File(plugin.getDataFolder(), "player_recipes");
        if (!dataFolder.exists()) dataFolder.mkdirs();
        this.gson = new GsonBuilder().setPrettyPrinting().create();
    }

    private File getFile(UUID uuid) {
        return new File(dataFolder, uuid.toString() + ".json");
    }

    // 非同期でロード
    public CompletableFuture<Set<String>> loadUnlockedRecipes(UUID uuid) {
        return CompletableFuture.supplyAsync(() -> {
            File file = getFile(uuid);
            if (!file.exists()) {
                return new HashSet<>();
            }

            try (Reader reader = new FileReader(file)) {
                Type setType = new TypeToken<HashSet<String>>(){}.getType();
                Set<String> data = gson.fromJson(reader, setType);
                return data != null ? data : new HashSet<>();
            } catch (IOException e) {
                e.printStackTrace();
                return new HashSet<>();
            }
        });
    }

    // 非同期で保存
    public void saveUnlockedRecipes(UUID uuid, Set<String> recipes) {
        CompletableFuture.runAsync(() -> {
            File file = getFile(uuid);
            try (Writer writer = new FileWriter(file)) {
                gson.toJson(recipes, writer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
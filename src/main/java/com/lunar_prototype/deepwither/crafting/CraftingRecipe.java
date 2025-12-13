package com.lunar_prototype.deepwither.crafting;

import java.util.Map;

public class CraftingRecipe {
    private final String id;
    private final String resultItemId;
    private final int timeSeconds;
    private final Map<String, Integer> ingredients; // Key: CustomID, Value: Amount

    public CraftingRecipe(String id, String resultItemId, int timeSeconds, Map<String, Integer> ingredients) {
        this.id = id;
        this.resultItemId = resultItemId;
        this.timeSeconds = timeSeconds;
        this.ingredients = ingredients;
    }

    public String getId() { return id; }
    public String getResultItemId() { return resultItemId; }
    public int getTimeSeconds() { return timeSeconds; }
    public Map<String, Integer> getIngredients() { return ingredients; }
}
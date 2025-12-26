package com.lunar_prototype.deepwither.crafting;

import com.lunar_prototype.deepwither.FabricationGrade;
import java.util.Map;

public class CraftingRecipe {
    private final String id;
    private final String resultItemId;
    private final int timeSeconds;
    private final Map<String, Integer> ingredients;
    private final FabricationGrade grade; // 追加: 製造等級

    public CraftingRecipe(String id, String resultItemId, int timeSeconds, Map<String, Integer> ingredients, FabricationGrade grade) {
        this.id = id;
        this.resultItemId = resultItemId;
        this.timeSeconds = timeSeconds;
        this.ingredients = ingredients;
        this.grade = grade;
    }

    public String getId() { return id; }
    public String getResultItemId() { return resultItemId; }
    public int getTimeSeconds() { return timeSeconds; }
    public Map<String, Integer> getIngredients() { return ingredients; }
    public FabricationGrade getGrade() { return grade; }
}
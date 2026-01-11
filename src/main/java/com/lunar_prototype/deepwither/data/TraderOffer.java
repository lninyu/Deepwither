package com.lunar_prototype.deepwither.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// ItemFactoryとの連携を可能にするため、カスタムアイテムの生成はOptional<ItemStack>を返す
public class TraderOffer {

    public enum ItemType { VANILLA, CUSTOM }

    private final String id;            // Custom ID または Material名
    private final ItemType itemType;    // VANILLA or CUSTOM
    private final int amount;           // アイテムの個数
    private final int cost;             // 購入時のVaultコスト
    private final int requiredCredit;   // 購入に必要な信用度

    // ロードされた ItemStack (カスタムアイテムの場合はNBT付き)
    private Optional<ItemStack> loadedItem;

    public TraderOffer(String id, ItemType itemType, int amount, int cost, int requiredCredit) {
        this.id = id;
        this.itemType = itemType;
        this.amount = amount;
        this.cost = cost;
        this.requiredCredit = requiredCredit;
        this.loadedItem = Optional.empty();
    }

    public int getAmount() {
        return amount;
    }

    public int getCost() {
        return cost;
    }

    public ItemType getItemType() {
        return itemType;
    }

    public int getRequiredCredit() {
        return requiredCredit;
    }

    public String getId() {
        return id;
    }

    public void setLoadedItem(ItemStack item) {
        this.loadedItem = Optional.of(item);
    }

    public Optional<ItemStack> getLoadedItem() {
        return loadedItem;
    }

    private List<ItemStack> requiredItems = new ArrayList<>();

    public List<ItemStack> getRequiredItems() { return requiredItems; }
    public void setRequiredItems(List<ItemStack> items) { this.requiredItems = items; }

    // ... 必要に応じてSellOfferと共通化/分離してください
}
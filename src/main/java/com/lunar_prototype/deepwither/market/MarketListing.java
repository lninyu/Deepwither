package com.lunar_prototype.deepwither.market;

import org.bukkit.inventory.ItemStack;
import java.util.UUID;

public class MarketListing {
    private final UUID id;
    private final UUID sellerId;
    private final ItemStack item;
    private final double price;
    private final long listedDate;

    public MarketListing(UUID sellerId, ItemStack item, double price) {
        this.id = UUID.randomUUID();
        this.sellerId = sellerId;
        this.item = item;
        this.price = price;
        this.listedDate = System.currentTimeMillis();
    }

    // Configからの読み込み用コンストラクタ
    public MarketListing(UUID id, UUID sellerId, ItemStack item, double price, long listedDate) {
        this.id = id;
        this.sellerId = sellerId;
        this.item = item;
        this.price = price;
        this.listedDate = listedDate;
    }

    public UUID getId() { return id; }
    public UUID getSellerId() { return sellerId; }
    public ItemStack getItem() { return item; }
    public double getPrice() { return price; }
    public long getListedDate() { return listedDate; }
}
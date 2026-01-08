package com.lunar_prototype.deepwither.util;

import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public final class InventoryHelper {
    private InventoryHelper() {}

    public static final int HOTBAR_HEAD = 0;
    public static final int HOTBAR_TAIL = 8;
    public static final int STORAGE_HEAD = 9;
    public static final int STORAGE_TAIL = 35;
    public static final InventoryStrategy MERGE_MAINHAND = (a, b) -> merge(b, a.getItemInMainHand());
    public static final InventoryStrategy MERGE_OFFHAND = (a, b) -> merge(b, a.getItemInOffHand());
    public static final InventoryStrategy MERGE_HOTBAR = createMergeStrategy(HOTBAR_HEAD, HOTBAR_TAIL);
    public static final InventoryStrategy MERGE_STORAGE = createMergeStrategy(STORAGE_HEAD, STORAGE_TAIL);
    public static final InventoryStrategy PLACE_HOTBAR = createPlaceStrategy(HOTBAR_HEAD, HOTBAR_TAIL);
    public static final InventoryStrategy PLACE_STORAGE = createPlaceStrategy(STORAGE_HEAD, STORAGE_TAIL);

    public static final List<InventoryStrategy> VANILLA = List.of(MERGE_MAINHAND, MERGE_OFFHAND, MERGE_HOTBAR, MERGE_STORAGE, PLACE_HOTBAR, PLACE_STORAGE);

    public static void merge(@NotNull ItemStack source, @NotNull ItemStack target) {
        var maxSize = source.getMaxStackSize();
        var targetSize = target.getAmount();

        if (source.isSimilar(target) && targetSize < maxSize) {
            var diff = maxSize - targetSize;
            var transfer = Math.min(source.getAmount(), diff);
            source.subtract(transfer);
            target.add(transfer);
        }
    }

    @Contract(pure = true)
    private static @NotNull InventoryStrategy createMergeStrategy(int head, int tail) {
        return (inventory, itemStack) -> {
            for (var i = head; i <= tail && !itemStack.isEmpty(); i++) {
                var stack = inventory.getItem(i);
                if (stack != null) merge(itemStack, stack);
            }
        };
    }

    @Contract(pure = true)
    private static @NotNull InventoryStrategy createPlaceStrategy(int head, int tail) {
        return (inventory, itemStack) -> {
            for (var i = head; i <= tail && !itemStack.isEmpty(); i++) {
                var stack = inventory.getItem(i);
                if (stack == null || stack.isEmpty()) {
                    inventory.setItem(i, itemStack.clone());
                    itemStack.setAmount(0);
                    break;
                }
            }
        };
    }

    public static void applyStrategy(@NotNull List<InventoryStrategy> collectors, @NotNull PlayerInventory inventory, @NotNull ItemStack itemStack) {
        for (var collector : collectors) {
            if (itemStack.isEmpty()) break;
            collector.apply(inventory, itemStack);
        }
    }

    /** {@code net.minecraft.client.multiplayer.ClientPacketListener#handleTakeItemEntity(ClientboundTakeItemEntityPacket)} (1.21.11 mojmap) */
    public static void playPickupSound(@NotNull Player player) {
        var random = ThreadLocalRandom.current();
        player.playSound(player, Sound.ENTITY_ITEM_PICKUP, SoundCategory.PLAYERS, 0.2f, (random.nextFloat() - random.nextFloat()) * 1.4f + 2.0f);
    }

    @FunctionalInterface
    public interface InventoryStrategy {
        void apply(@NotNull PlayerInventory inventory, @NotNull ItemStack itemStack);
    }
}

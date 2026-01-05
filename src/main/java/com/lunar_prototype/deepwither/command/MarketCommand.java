package com.lunar_prototype.deepwither.command;

import com.lunar_prototype.deepwither.Deepwither;
import com.lunar_prototype.deepwither.market.GlobalMarketManager;
import com.lunar_prototype.deepwither.market.MarketGui;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class MarketCommand implements CommandExecutor, TabCompleter {

    private final Deepwither plugin;
    private final GlobalMarketManager marketManager;
    private final MarketGui marketGui;

    public MarketCommand(Deepwither plugin, GlobalMarketManager marketManager, MarketGui marketGui) {
        this.plugin = plugin;
        this.marketManager = marketManager;
        this.marketGui = marketGui;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cこのコマンドはプレイヤーのみ実行可能です。");
            return true;
        }

        // 引数なしの場合: メインメニューを開く
        if (args.length == 0) {
            marketGui.openMainMenu(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "sell" -> handleSell(player, args);
            case "collect" -> handleCollect(player);
            case "help" -> sendHelp(player);
            default -> {
                player.sendMessage("§c不明なサブコマンドです。 /market help で確認してください。");
                marketGui.openMainMenu(player); // 親切設計: 間違えたらGUIを開いてあげる
            }
        }

        return true;
    }

    /**
     * 出品処理 (/market sell <price>)
     */
    private void handleSell(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage("§c使用法: /market sell <価格>");
            return;
        }

        ItemStack handItem = player.getInventory().getItemInMainHand();

        if (handItem.getType() == Material.AIR) {
            player.sendMessage("§cメインハンドにアイテムを持っていません。");
            return;
        }

        double price;
        try {
            price = Double.parseDouble(args[1]);
        } catch (NumberFormatException e) {
            player.sendMessage("§c価格は数値で入力してください。");
            return;
        }

        if (price <= 0) {
            player.sendMessage("§c価格は 0 より大きい必要があります。");
            return;
        }

        // アイテムを出品リストに追加
        marketManager.listItem(player, handItem, price);

        // インベントリからアイテムを削除 (複製防止のため最優先)
        player.getInventory().setItemInMainHand(null);

        player.sendMessage("§a[Market] アイテムを " + price + " G で出品しました！");
        player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
    }

    /**
     * 売上金回収処理 (/market collect)
     */
    private void handleCollect(Player player) {
        marketManager.claimEarnings(player);
    }

    /**
     * ヘルプ表示
     */
    private void sendHelp(Player player) {
        player.sendMessage("§6========== [ Global Market ] ==========");
        player.sendMessage("§e/market §7- マーケットメニューを開く");
        player.sendMessage("§e/market sell <価格> §7- 手に持ったアイテムを出品する");
        player.sendMessage("§e/market collect §7- 売上金を回収する");
        player.sendMessage("§6=======================================");
    }

    // --- Tab補完 (入力補助) ---
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        List<String> suggestions = new ArrayList<>();

        if (args.length == 1) {
            suggestions.add("sell");
            suggestions.add("collect");
            suggestions.add("help");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("sell")) {
            suggestions.add("100");
            suggestions.add("500");
            suggestions.add("1000");
        }

        return suggestions;
    }
}
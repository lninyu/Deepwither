package com.lunar_prototype.deepwither;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreditCommand implements CommandExecutor {

    private final CreditManager creditManager;

    public CreditCommand(CreditManager creditManager) {
        this.creditManager = creditManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("deepwither.credit.admin")) {
            sender.sendMessage(ChatColor.RED + "権限がありません。");
            return true;
        }

        if (args.length < 3) {
            sender.sendMessage(ChatColor.YELLOW + "使用方法: /credit <プレイヤー名> <トレーダーID> <増減量>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
            return true;
        }

        String traderId = args[1];
        int amount;
        try {
            amount = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "増減量は数値で指定してください。");
            return true;
        }

        creditManager.addCredit(target.getUniqueId(), traderId, amount);

        int currentCredit = creditManager.getCredit(target.getUniqueId(), traderId);

        sender.sendMessage(ChatColor.GREEN + target.getName() + "のトレーダー[" + traderId + "]に対する信用度を " + amount + " 更新しました。");
        sender.sendMessage(ChatColor.GREEN + "現在の信用度: " + currentCredit);

        if (target != sender) {
            target.sendMessage(ChatColor.YELLOW + "トレーダー[" + traderId + "]に対する信用度が " + amount + " 変化しました。現在: " + currentCredit);
        }

        return true;
    }
}
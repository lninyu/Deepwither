package com.lunar_prototype.deepwither;

import com.lunar_prototype.deepwither.party.Party;
import com.lunar_prototype.deepwither.party.PartyManager;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class PartyCommand implements CommandExecutor {

    private final PartyManager partyManager;

    public PartyCommand(PartyManager partyManager) {
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cプレイヤーのみ実行可能です。");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "invite":
                handleInvite(player, args);
                break;
            case "accept":
                partyManager.acceptInvite(player);
                break;
            case "leave":
                partyManager.leaveParty(player);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "disband":
                partyManager.disbandParty(player);
                break;
            case "info":
                handleInfo(player);
                break;
            default:
                sendHelp(player);
                break;
        }

        return true;
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "使用法: /party invite <プレイヤー名>");
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(ChatColor.RED + "プレイヤーが見つかりません。");
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(ChatColor.RED + "自分自身は招待できません。");
            return;
        }

        // 既に相手がパーティーに入っているか
        if (partyManager.getParty(target) != null) {
            player.sendMessage(ChatColor.RED + "そのプレイヤーは既にパーティーに参加しています。");
            return;
        }

        // 自分がパーティー未加入なら、ここで新規作成（実質リーダーになる）
        if (partyManager.getParty(player) == null) {
            partyManager.createParty(player);
        } else {
            // 自分がリーダーか確認
            Party party = partyManager.getParty(player);
            if (!party.getLeaderId().equals(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "招待権限がありません（リーダーのみ）。");
                return;
            }
        }

        partyManager.invitePlayer(player, target);
        player.sendMessage(ChatColor.GREEN + target.getName() + " に招待を送りました。");

        // ターゲットにクリック可能なメッセージを送信
        target.sendMessage(ChatColor.GOLD + "==============================");
        target.sendMessage(ChatColor.AQUA + player.getName() + " からパーティー招待が届きました！");

        TextComponent message = new TextComponent("§a§l[ここをクリックして参加]");
        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/party accept"));
        target.spigot().sendMessage(message);

        target.sendMessage(ChatColor.GRAY + "(または /party accept と入力)");
        target.sendMessage(ChatColor.GOLD + "==============================");
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "使用法: /party kick <プレイヤー名>");
            return;
        }
        partyManager.kickMember(player, args[1]);
    }

    private void handleInfo(Player player) {
        Party party = partyManager.getParty(player);
        if (party == null) {
            player.sendMessage(ChatColor.RED + "パーティーに参加していません。");
            return;
        }

        player.sendMessage(ChatColor.BLUE + "=== パーティー情報 ===");

        UUID leaderId = party.getLeaderId();
        Player leader = Bukkit.getPlayer(leaderId);
        String leaderName = (leader != null) ? leader.getName() : "Unknown";

        player.sendMessage(ChatColor.GOLD + "リーダー: " + leaderName);
        player.sendMessage(ChatColor.WHITE + "メンバー (" + party.getMemberIds().size() + "名):");

        for (UUID memberId : party.getMemberIds()) {
            Player p = Bukkit.getPlayer(memberId);
            String name = (p != null && p.isOnline()) ? "§a" + p.getName() : "§7(Offline)";
            player.sendMessage(" - " + name);
        }
        player.sendMessage(ChatColor.BLUE + "==================");
    }

    private void sendHelp(Player player) {
        player.sendMessage(ChatColor.YELLOW + "--- パーティーコマンド ---");
        player.sendMessage("§b/party invite <player> §7- プレイヤーを招待");
        player.sendMessage("§b/party accept §7- 招待を受ける");
        player.sendMessage("§b/party leave §7- パーティーから脱退");
        player.sendMessage("§b/party info §7- メンバーを表示");
        player.sendMessage("§c/party kick <player> §7- メンバーを追放(リーダーのみ)");
        player.sendMessage("§c/party disband §7- パーティー解散(リーダーのみ)");
    }
}
package com.lunar_prototype.deepwither;

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

public class SafeZoneListener implements Listener {

    // プレイヤーがブロックを跨いでいない移動は無視する
    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();

        // ブロック座標が同じであれば処理しない（パフォーマンス対策）
        if (from.getBlockX() == to.getBlockX() &&
                from.getBlockY() == to.getBlockY() &&
                from.getBlockZ() == to.getBlockZ()) {
            return;
        }

        // WorldGuardが有効でなければ何もしない
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return;
        }

        boolean isOldInSafeZone = isSafeZone(from);
        boolean isNewInSafeZone = isSafeZone(to);

        // ----------------------------------------------------
        // ★ 1. セーフゾーンへの侵入をチェック
        // ----------------------------------------------------
        if (isNewInSafeZone && !isOldInSafeZone) {
            // セーフゾーンに侵入
            player.sendTitle(
                    ChatColor.AQUA + "セーフゾーン",
                    ChatColor.GRAY + "",
                    10, // フェードイン (0.5秒)
                    70, // 滞在時間 (3.5秒)
                    20  // フェードアウト (1.0秒)
            );
            player.sendMessage(ChatColor.AQUA + ">> セーフゾーンに侵入しました。");
        }

        // ----------------------------------------------------
        // ★ 2. セーフゾーンからの退出をチェック (オプション)
        // ----------------------------------------------------
        else if (!isNewInSafeZone && isOldInSafeZone) {
            // セーフゾーンから退出
            player.sendTitle(
                    ChatColor.RED + "危険区域",
                    ChatColor.GRAY + "",
                    10, 70, 20
            );
            player.sendMessage(ChatColor.RED + ">> 危険区域へ移動しました。");
        }
    }

    /**
     * 指定されたLocationが、名前に「safezone」を含むリージョン内にあるかを判定します。
     */
    private boolean isSafeZone(Location loc) {
        // WorldGuardのAPI経由でリージョンコンテナを取得
        RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();

        // クエリを作成し、現在の場所がどのリージョンに含まれるかを取得
        RegionQuery query = container.createQuery();
        ApplicableRegionSet set = query.getApplicableRegions(BukkitAdapter.adapt(loc));

        // 適用可能なリージョンを全てチェック
        for (ProtectedRegion region : set) {
            // リージョンID（名前）が「safezone」を含んでいるか（大文字小文字を無視）
            if (region.getId().toLowerCase().contains("safezone")) {
                return true;
            }
        }
        return false;
    }
}
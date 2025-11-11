package com.lunar_prototype.deepwither;

import io.lumine.mythic.api.exceptions.InvalidMobTypeException;
import io.lumine.mythic.bukkit.MythicBukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerAnimationType;
import org.bukkit.inventory.ItemStack;

public class PlayerAnimationListener implements Listener {

    @EventHandler
    public void onPlayerArmSwing(PlayerAnimationEvent event) {
        Player player = event.getPlayer();

        // 1. 腕振りアニメーション (近接攻撃/空振り) のみをチェック
        if (event.getAnimationType() != PlayerAnimationType.ARM_SWING) {
            return;
        }

        ItemStack mainHand = player.getInventory().getItemInMainHand();

        // 2. メインハンドが近接武器であるか（剣など）をチェック
        // ただし、全てのアイテムでスキルを発動させたい場合は、このチェックを緩める
        if (mainHand == null || mainHand.getType() == Material.AIR) {
            return;
        }

        // ----------------------------------------------------
        // ★ MythicMobs スキル呼び出しのトリガー
        // ----------------------------------------------------

        // ここでは、武器に付与されたカスタムデータ（LoreやPDC）からスキル名と確率を取得することを推奨します。
        // シンプル化のため、今回は固定のスキル名と確率を使用します。
        final String MM_SKILL_NAME = "turquoise_slash";
        // スキルを実行
        // Caster: Player, ターゲット: なし (プレイヤーを中心としたAOEスキルなどを想定)
        MythicBukkit.inst().getAPIHelper().castSkill(player, MM_SKILL_NAME);
    }
}
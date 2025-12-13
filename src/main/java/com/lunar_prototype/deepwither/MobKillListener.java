package com.lunar_prototype.deepwither;

import com.lunar_prototype.deepwither.outpost.OutpostEvent;
import com.lunar_prototype.deepwither.outpost.OutpostManager;
import com.lunar_prototype.deepwither.party.Party;          // ★追加
import com.lunar_prototype.deepwither.party.PartyManager;   // ★追加
import io.lumine.mythic.bukkit.events.MythicMobDeathEvent;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerExpChangeEvent;

import java.util.List;
import java.util.stream.Collectors;

public class MobKillListener implements Listener {
    private final LevelManager levelManager;
    private final FileConfiguration mobExpConfig;
    private final OutpostManager outpostManager;
    private final PartyManager partyManager; // ★追加

    // ★ コンストラクタにPartyManagerを追加
    public MobKillListener(LevelManager levelManager, FileConfiguration config, OutpostManager outpostManager, PartyManager partyManager) {
        this.levelManager = levelManager;
        this.mobExpConfig = config;
        this.outpostManager = outpostManager;
        this.partyManager = partyManager; // ★初期化
    }

    @EventHandler
    public void onMythicMobDeath(MythicMobDeathEvent e) {
        if (!(e.getKiller() instanceof Player killer)) return;

        // 1. 経験値処理
        String mobType = e.getMobType().getInternalName();
        double baseExp = mobExpConfig.getDouble("mob-exp." + mobType, 0);

        if (baseExp > 0) {
            handleExpDistribution(killer, baseExp); // ★ 分配メソッド呼び出しに変更
        }
        // updatePlayerDisplayは分配メソッド内で行うか、ここでkillerだけ更新するかですが、
        // 分配対象全員を更新する必要があるためメソッド内で処理します。

        // 2. Outpost Mobの撃破カウント (既存ロジック・変更なし)
        OutpostEvent activeEvent = outpostManager.getActiveEvent();
        if (activeEvent != null) {
            String mobOutpostId = Deepwither.getInstance().getMobSpawnManager().getMobOutpostId(e.getEntity());
            if (mobOutpostId != null && mobOutpostId.equals(activeEvent.getOutpostRegionId())) {
                activeEvent.mobDefeated(e.getEntity(), killer.getUniqueId());
            }
        }
    }

    /**
     * ★ 経験値分配ロジック
     */
    private void handleExpDistribution(Player killer, double baseExp) {
        Party party = partyManager.getParty(killer);

        // パーティーに入っていない場合は通常通り
        if (party == null) {
            levelManager.addExp(killer, baseExp);
            levelManager.updatePlayerDisplay(killer);
            return;
        }

        // --- パーティー分配処理 ---
        double shareRadius = 30.0; // 経験値を共有する範囲（config化推奨）

        // 範囲内にいるメンバーを取得（キラー本人も含む）
        List<Player> nearbyMembers = party.getOnlineMembers().stream()
                .filter(p -> p.getWorld().equals(killer.getWorld())) // 同じワールド
                .filter(p -> p.getLocation().distanceSquared(killer.getLocation()) <= shareRadius * shareRadius) // 距離チェック
                .collect(Collectors.toList());

        if (nearbyMembers.isEmpty()) {
            // 万が一誰もいない場合（ありえないが安全策）
            levelManager.addExp(killer, baseExp);
            levelManager.updatePlayerDisplay(killer);
            return;
        }

        // 分配計算
        // 例: メンバー数に応じたボーナス (2人なら1.1倍, 3人なら1.2倍...)
        double partyBonusMultiplier = 1.0 + ((nearbyMembers.size() - 1) * 0.1);
        double totalExpWithBonus = baseExp * partyBonusMultiplier;

        // 一人当たりの経験値（均等割り）
        double expPerMember = totalExpWithBonus / nearbyMembers.size();

        // メンバー全員に付与
        for (Player member : nearbyMembers) {
            levelManager.addExp(member, expPerMember);
            levelManager.updatePlayerDisplay(member);

            // オプション: 経験値獲得のログを出すならここ
            // member.sendMessage("§e[Party] +" + String.format("%.1f", expPerMember) + " Exp");
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerExpChange(PlayerExpChangeEvent event) {
        Player player = event.getPlayer();
        event.setAmount(0);
        levelManager.updatePlayerDisplay(player);
    }
}
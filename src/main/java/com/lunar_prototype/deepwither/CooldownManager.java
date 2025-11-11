package com.lunar_prototype.deepwither;

import org.bukkit.Bukkit;
import com.lunar_prototype.deepwither.StatType; // StatTypeへのパスが正しいことを確認

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {

    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();

    // ★ 修正: minCooldown を引数に追加
    public boolean isOnCooldown(UUID uuid, String skillId, double baseCooldown, double minCooldown) {
        // ★ minCooldown を渡す
        double actualCooldown = applyCooldownReduction(uuid, baseCooldown, minCooldown);

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(uuid, new HashMap<>()).getOrDefault(skillId, 0L);

        // actualCooldown は秒単位なので、1000を掛けてミリ秒にする
        return (now - last) < (actualCooldown * 1000);
    }

    // ★ 修正: minCooldown を引数に追加
    public double getRemaining(UUID uuid, String skillId, double baseCooldown, double minCooldown) {
        // ★ minCooldown を渡す
        double actualCooldown = applyCooldownReduction(uuid, baseCooldown, minCooldown);

        long now = System.currentTimeMillis();
        long last = cooldowns.getOrDefault(uuid, new HashMap<>()).getOrDefault(skillId, 0L);

        double remaining = (actualCooldown * 1000 - (now - last)) / 1000.0;
        return Math.max(0, remaining);
    }

    // ★ 修正: minCooldown を引数に追加
    private double applyCooldownReduction(UUID uuid, double baseCooldown, double minCooldown) {
        // 1. クールダウン短縮ステータスを取得 (COOLDOWN_REDUCTION stat type が存在すると仮定)
        double reduction = StatManager.getTotalStatsFromEquipment(Bukkit.getPlayer(uuid)).getFlat(StatType.COOLDOWN_REDUCTION);

        // 2. 短縮率の安全な上限を適用 (例: 90%)
        reduction = Math.min(reduction, 0.9);

        // 3. 短縮後のクールダウン時間を計算
        double reducedCooldown = baseCooldown * (1.0 - reduction);

        // 4. ★ 最小クールダウン時間 (min) を適用
        // reducedCooldown が minCooldown より小さければ、minCooldown にする
        return Math.max(reducedCooldown, minCooldown);
    }

    // setCooldown はクールダウン開始時刻を記録するだけなので変更なし
    public void setCooldown(UUID uuid, String skillId) {
        cooldowns.computeIfAbsent(uuid, k -> new HashMap<>())
                .put(skillId, System.currentTimeMillis());
    }
}
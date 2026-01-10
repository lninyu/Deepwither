package com.lunar_prototype.deepwither.seeker;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.core.mobs.ActiveMob;
import io.papermc.paper.event.player.PlayerArmSwingEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.Optional;

public class CombatExperienceListener implements Listener {
    private final SeekerAIEngine aiEngine;

    public CombatExperienceListener(SeekerAIEngine aiEngine) {
        this.aiEngine = aiEngine;
    }

    @EventHandler
    public void onCombat(EntityDamageByEntityEvent event) {
        double damage = event.getFinalDamage();

        // --- 1. Mobがダメージを与えた場合 (成功体験) ---
        if (event.getDamager() instanceof Mob) {
            Mob attacker = (Mob) event.getDamager();

            // 与ダメージ報酬（ターゲットへの攻撃成功）
            // 第4引数: evaded=false, 第5引数: damager=null (自分が攻撃側なので不要)
            applyLearning(attacker, damage, 0.0, false, null);

            // 既存のReward処理
            handleReward(attacker, 0.3);
            getBrain(attacker).ifPresent(brain -> brain.tacticalMemory.myHits++);
        }

        // --- 2. Mobがダメージを受けた場合 (失敗体験 & 複数戦の学習) ---
        if (event.getEntity() instanceof Mob) {
            Mob victim = (Mob) event.getEntity();
            Entity damager = event.getDamager();

            // Q-Learning: ダメージを受けた報酬処理
            // 第5引数に damager を渡すことで、内部で「ターゲット以外からの被弾（横槍）」を判定可能にする
            applyLearning(victim, 0.0, damage, false, damager);

            // 被弾による戦術メモリーの更新
            getBrain(victim).ifPresent(brain -> brain.tacticalMemory.takenHits++);

            // 既存のPenalty処理
            handlePenaltyAndPattern(victim, damager, 0.5);

            // 相手がプレイヤーなら攻撃パターンを詳細に記録
            if (damager instanceof Player) {
                Player p = (Player) damager;
                getBrain(victim).ifPresent(brain -> {
                    double dist = p.getLocation().distance(victim.getLocation());
                    // 被弾した瞬間、ターゲットが遠くにいるなら、この攻撃者へ即座にヘイトを向ける検討
                    if (victim.getTarget() == null || victim.getLocation().distance(victim.getTarget().getLocation()) > 10.0) {
                        if (Math.random() < 0.5) victim.setTarget(p); // 50%で反撃対象を切り替え
                    }

                    // 攻撃パターンのサンプリング
                    brain.recordAttack(p.getUniqueId(), victim.getTicksLived(), dist, false, null, null);
                });
            }
        }
    }

    private void handleReward(Entity entity, double amount) {
        getBrain(entity).ifPresent(brain -> {
            brain.accumulatedReward += amount;
            brain.recordSelfAttack(entity.getTicksLived());
        });
    }

    /**
     * 失敗体験の蓄積に加え、V2用の攻撃リズム・距離学習を実行する
     */
    private void handlePenaltyAndPattern(Entity victim, Entity damager, double amount) {
        if (!(victim instanceof Mob)) return;
        Mob mob = (Mob) victim;

        getBrain(mob).ifPresent(brain -> {
            // 失敗体験（ペナルティ）の蓄積
            brain.accumulatedPenalty += amount;

            // --- V2機能: 攻撃パターンの記録 ---
            // 攻撃者がプレイヤーの場合のみ、その距離と時間を脳に記録する
            if (damager instanceof Player) {
                Player player = (Player) damager;
                double distance = player.getLocation().distance(mob.getLocation());
                long currentTick = mob.getTicksLived(); // 個体の生存Tickを時間軸として使用
                Vector toMob = mob.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();

                brain.recordAttack(player.getUniqueId(), currentTick, distance,false,player.getLocation().getDirection(),toMob);
            }
        });
    }

    @EventHandler
    public void onSwing(PlayerArmSwingEvent event) {
        Player player = event.getPlayer();

        player.getNearbyEntities(8, 8, 8).stream()
                .filter(e -> e instanceof Mob)
                .filter(e -> MythicBukkit.inst().getMobManager().isActiveMob(e.getUniqueId()))
                .forEach(e -> {
                    Mob mob = (Mob) e;
                    Optional.ofNullable(aiEngine.getBrain(mob.getUniqueId())).ifPresent(brain -> {
                        Vector playerLoc = player.getLocation().toVector();
                        Vector mobLoc = mob.getLocation().toVector();
                        Vector toMob = mobLoc.clone().subtract(playerLoc).normalize();
                        Vector lookDir = player.getLocation().getDirection();
                        double dist = player.getLocation().distance(mob.getLocation());

                        // --- [追加] 回避成功の学習 ---
                        // プレイヤーがMobをしっかり狙っている（dot > 0.9）のに当たっていない場合
                        if (lookDir.dot(toMob) > 0.9) {
                            // 回避成功報酬（0.8程度）を与える
                            // これにより「EVADE」や「BURST_DASH」のQ値が跳ね上がる
                            applyLearning(mob, 0.0, 0.0, true);
                            brain.tacticalMemory.avoidedHits++; // 回避カウンター増
                        }

                        // 既存の空振りリズム学習
                        brain.recordAttack(player.getUniqueId(), mob.getTicksLived(), dist, true, lookDir, toMob);
                    });
                });
    }

    private Optional<LiquidBrain> getBrain(Entity entity) {
        return MythicBukkit.inst().getMobManager().getActiveMob(entity.getUniqueId())
                .map(am -> aiEngine.getBrain(am.getUniqueId()));
    }

    // A. 通常のダメージ・回避用（引数4つ）
    public void applyLearning(Mob mob, double damageDealt, double damageTaken, boolean evaded) {
        // 内部的に damager = null としてメインロジックを呼ぶ
        applyLearning(mob, damageDealt, damageTaken, evaded, null);
    }

    // B. メインロジック：横槍判定（damager）を含む完全版
    public void applyLearning(Mob mob, double damageDealt, double damageTaken, boolean evaded, Entity damager) {
        getBrain(mob).ifPresent(brain -> {
            double reward = 0.0;

            // 1. 基本報酬
            reward += (damageDealt * 2.5);
            reward -= (damageTaken * 1.5);
            if (evaded) reward += 1.5;

            // 2. 横槍ペナルティ (ターゲット以外からの被弾)
            if (damageTaken > 0 && damager != null) {
                Entity target = mob.getTarget();
                if (target != null && !damager.getUniqueId().equals(target.getUniqueId())) {
                    reward -= 5.0; // 強烈な反省
                    brain.frustration += 0.2; // ストレス増で探索率アップ
                }
            }

            // 3. 槍の間合い突破ボーナス
            if (mob.getTarget() instanceof Player) {
                double dist = mob.getLocation().distance(mob.getTarget().getLocation());
                if (dist < 3.0 && damageTaken < 2.0) {
                    reward += 1.0;
                }
            }

            // 4. 次の状態(Next State)の取得
            // SensorProvider経由で最新の敵リストを取得してKeyを生成
            List<BanditContext.EnemyInfo> enemies = new SensorProvider().scanEnemies(mob, mob.getNearbyEntities(32, 32, 32));
            String nextState = brain.qTable.getStateKey(
                    brain.tacticalMemory.combatAdvantage,
                    mob.getTarget() != null ? mob.getLocation().distance(mob.getTarget().getLocation()) : 20.0,
                    false,
                    enemies
            );

            // 5. 学習更新
            if (brain.lastStateKey != null && brain.lastActionType != null) {
                brain.qTable.update(brain.lastStateKey, brain.lastActionType, reward, nextState);
            }
        });
    }
}
package com.lunar_prototype.deepwither;

import io.lumine.mythic.bukkit.MythicBukkit;
import io.lumine.mythic.bukkit.events.MythicDamageEvent;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority; // 追加
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityRegainHealthEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.*;

public class DamageManager implements Listener {

    private final Set<UUID> isProcessingDamage = new HashSet<>();
    private final StatManager statManager;
    private final Map<UUID, Long> onHitCooldowns = new HashMap<>();

    // 定数定義
    private static final Set<String> UNDEAD_MOB_IDS = Set.of("melee_skeleton", "ranged_skeleton","melee_zombi");
    private static final double MAGIC_DEFENSE_DIVISOR = 100.0;
    private static final double DEFENSE_DIVISOR = 100.0; // 物理防御用
    private static final double HEAVY_DEFENSE_DIVISOR = 500.0; // 高耐久用

    // 盾のクールダウン (ms) - 連続ブロック防止用など
    private static final long SHIELD_COOLDOWN_MS = 500;
    private final Map<UUID, Long> lastShieldBlockTime = new HashMap<>();

    public DamageManager(StatManager statManager) {
        this.statManager = statManager;
    }

    private final Map<UUID, Map<UUID, MagicHitInfo>> magicHitMap = new HashMap<>();

    private static class MagicHitInfo {
        int hitCount;
        long lastHitTime;

        MagicHitInfo(int hitCount, long lastHitTime) {
            this.hitCount = hitCount;
            this.lastHitTime = lastHitTime;
        }
    }

    // ----------------------------------------------------
    // --- A. Mythic Mobs 魔法ダメージ処理 ---
    // ----------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onMythicDamage(MythicDamageEvent e) {
        if (!(e.getCaster().getEntity().getBukkitEntity() instanceof Player player)) return;
        if (!(e.getTarget().getBukkitEntity() instanceof LivingEntity targetLiving)) return;

        // 特攻タグ処理
        if (e.getDamageMetadata().getTags().contains("UNDEAD")) {
            if (handleUndeadDamage(player, targetLiving, e)) return;
        }

        // LIFESTEAL処理
        if (e.getDamageMetadata().getTags().contains("LIFESTEAL")) {
            handleLifesteal(player, targetLiving, e.getDamage());
        }

        if (e.getDamageMetadata().getDamageCause() != EntityDamageEvent.DamageCause.ENTITY_EXPLOSION) {
            return;
        }

        StatMap attackerStats = StatManager.getTotalStatsFromEquipment(player);
        StatMap defenderStats = getDefenderStats(targetLiving);

        // 基本魔法ダメージ計算
        double baseDamage = e.getDamage() + attackerStats.getFinal(StatType.MAGIC_DAMAGE);

        // クリティカル判定
        boolean isCrit = rollChance(attackerStats.getFinal(StatType.CRIT_CHANCE));
        if (isCrit) {
            baseDamage *= (attackerStats.getFinal(StatType.CRIT_DAMAGE) / 100.0);
            player.sendMessage("§6§l魔法クリティカル！");
        }

        // AOEやBurstダメージの場合は通常の魔法ダメージを減衰させてそれ+バースト,AOEダメージステータスをプラスした値をダメージとする
        double bonusDamage = 0;
        boolean isBurst = e.getDamageMetadata().getTags().contains("BURST");
        boolean isAoe = e.getDamageMetadata().getTags().contains("AOE");

        if (isBurst) {
            baseDamage *= 0.4;
            bonusDamage += attackerStats.getFinal(StatType.MAGIC_BURST_DAMAGE);
        }
        if (isAoe) {
            baseDamage *= 0.6;
            bonusDamage += attackerStats.getFinal(StatType.MAGIC_AOE_DAMAGE);
        }
        baseDamage += bonusDamage;

        // 防御計算
        double finalDamage = applyDefense(baseDamage, defenderStats.getFinal(StatType.MAGIC_RESIST), MAGIC_DEFENSE_DIVISOR);

        finalDamage = applyMultiHitDecay(player,targetLiving,finalDamage);

        // PvPチェック
        if (targetLiving instanceof Player && isPvPPrevented(player, targetLiving)) {
            e.setCancelled(true);
            return;
        }

        // メッセージ表示
        String typeMsg = isAoe ? "§b§l魔法AOEダメージ！" : (isBurst ? "§c§l魔法バーストダメージ！" : "§b§l魔法ダメージ！");
        player.sendMessage(typeMsg + " §c+" + Math.round(finalDamage));

        e.setDamage(0.0);
        applyCustomDamage(targetLiving, finalDamage, player);
    }

    // ----------------------------------------------------
    // --- B. 物理ダメージ処理 (近接 & 遠距離 & 盾) ---
    // ----------------------------------------------------
    @EventHandler(priority = EventPriority.HIGH)
    public void onPhysicalDamage(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) return;

        // 攻撃者の特定 (プレイヤーのみ処理)
        Player attacker = null;
        boolean isProjectile = false;
        Projectile projectileEntity = null;

        if (e.getDamager() instanceof Player p) {
            attacker = p;
        } else if (e.getDamager() instanceof Projectile proj && proj.getShooter() instanceof Player p) {
            attacker = p;
            isProjectile = true;
            projectileEntity = proj;
        }

        if (attacker == null) return;
        if (isProcessingDamage.contains(attacker.getUniqueId())) return;
        if (!(e.getEntity() instanceof LivingEntity targetLiving)) return;

        // 爆発などは除外
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) return;

        // PvPチェック
        if (targetLiving instanceof Player && isPvPPrevented(attacker, targetLiving)) {
            e.setCancelled(true);
            return;
        }

        // ステータス取得
        StatMap attackerStats = StatManager.getTotalStatsFromEquipment(attacker);
        StatMap defenderStats = getDefenderStats(targetLiving);

        // --- ダメージ計算 ---
        double attackPower = isProjectile ? attackerStats.getFinal(StatType.PROJECTILE_DAMAGE) : attackerStats.getFinal(StatType.ATTACK_DAMAGE);
        double baseDamage = attackPower + (isProjectile ? e.getDamage() : 0); // 弓は基本ダメージ加算

        // クリティカル
        boolean isCrit = rollChance(attackerStats.getFinal(StatType.CRIT_CHANCE));
        if (isCrit) {
            baseDamage *= (attackerStats.getFinal(StatType.CRIT_DAMAGE) / 100.0);
        }

        // 距離補正 (遠距離のみ)
        double distMult = 1.0;
        if (isProjectile) {
            distMult = calculateDistanceMultiplier(attacker, targetLiving);
            baseDamage *= distMult;
        }

        // 防御計算
        double finalDamage = applyDefense(baseDamage, defenderStats.getFinal(StatType.DEFENSE), DEFENSE_DIVISOR);

        // クールダウン減衰 (近接のみ)
        if (!isProjectile) {
            float cooldown = attacker.getAttackCooldown();
            if (cooldown < 1.0f) {
                double reduced = finalDamage * (1.0 - cooldown);
                finalDamage *= cooldown;
                attacker.sendMessage("§c攻撃クールダウン！ §c-" + Math.round(reduced) + " §7ダメージ §a(" + Math.round(finalDamage) + ")");
            }
        }

        // ★★★ 盾による軽減処理 ★★★
        if (targetLiving instanceof Player defenderPlayer && defenderPlayer.isBlocking()) {
            // 正面からの攻撃かチェック
            Vector attackerDir = attacker.getLocation().toVector().subtract(defenderPlayer.getLocation().toVector()).normalize();
            Vector defenderDir = defenderPlayer.getLocation().getDirection();
            if (attackerDir.dot(defenderDir) < 0) { // 内積が負なら向き合っている(前方)
                // 盾の軽減率を取得 (SHIELD_BLOCK_RATE: 0.0=0% ~ 1.0=100%カット)
                double blockRate = defenderStats.getFinal(StatType.SHIELD_BLOCK_RATE);

                // デフォルト軽減率 (例: 0.2 = 20%軽減) を設定してもよい
                blockRate = Math.max(0.0, Math.min(blockRate, 1.0));

                double blockedDamage = finalDamage * blockRate;
                finalDamage -= blockedDamage;

                defenderPlayer.getWorld().playSound(defenderPlayer.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
                defenderPlayer.sendMessage("§b盾防御！ §7軽減: §a" + Math.round(blockedDamage) + " §c(" + Math.round(finalDamage) + "被弾)");

                // バニラの盾SE/ノックバックイベントを防ぐ必要があればここで調整
            }
        }

        // 最終調整
        finalDamage = Math.max(0.1, finalDamage);

        // イベントキャンセル & ダメージ適用
        e.setDamage(0.0);
        if (isProjectile) projectileEntity.remove(); // 矢を消す

        // エフェクト & メッセージ
        if (isCrit) {
            attacker.sendMessage("§6§lクリティカル！ §c+" + Math.round(finalDamage));
            attacker.getWorld().spawnParticle(Particle.CRIT, targetLiving.getLocation().add(0, 1, 0), 10, 0.3, 0.3, 0.3, 0.1);
            attacker.getWorld().playSound(attacker.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 1f);
        } else if (isProjectile) {
            attacker.sendMessage("§7遠距離命中 §c+" + Math.round(finalDamage) + " §e[" + String.format("%.0f%%", distMult * 100) + "]");
        }

        applyCustomDamage(targetLiving, finalDamage, attacker);
        tryTriggerOnHitSkill(attacker, targetLiving, attacker.getInventory().getItemInMainHand());

        // 槍の貫通処理 (近接のみ)
        if (!isProjectile && isSpearWeapon(attacker.getInventory().getItemInMainHand())) {
            handleSpearCleave(attacker, targetLiving, finalDamage);
        }
    }

    // ----------------------------------------------------
    // --- C. 被ダメージ処理 (環境・Mob・その他) ---
    // ----------------------------------------------------
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerReceivingDamage(EntityDamageEvent e) {
        if (!(e.getEntity() instanceof Player player)) return;
        if (e.isCancelled()) return;

        // Player攻撃によるEntityDamageByEntityEventは既に処理済みだが、
        // バニラロジック等で漏れた場合や環境ダメージをここで拾う

        // ダメージソース特定
        LivingEntity attacker = null;
        if (e instanceof EntityDamageByEntityEvent ev) {
            if (ev.getDamager() instanceof LivingEntity le) attacker = le;
            else if (ev.getDamager() instanceof Projectile p && p.getShooter() instanceof LivingEntity le) attacker = le;

            // 攻撃者がプレイヤーの場合は既に処理済みのはずなのでスキップ
            if (attacker instanceof Player) return;
        }

        double incomingDamage = e.getFinalDamage();
        StatMap defenderStats = StatManager.getTotalStatsFromEquipment(player);

        // 魔法(爆発)ダメージ処理
        if (e.getCause() == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION ||
                e.getCause() == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION) {

            double reduced = applyDefense(incomingDamage, defenderStats.getFinal(StatType.MAGIC_RESIST), MAGIC_DEFENSE_DIVISOR);
            player.sendMessage("§5§l魔法被弾！ §c" + String.format("%.1f", reduced));

            e.setDamage(0.0);
            processPlayerDamageWithAbsorption(player, reduced, attacker != null ? attacker.getName() : "魔法");
            return;
        }

        // Mobからの攻撃処理 (クリティカル等)
        if (attacker instanceof Mob) {
            incomingDamage = handleMobDamageLogic(attacker, incomingDamage, player);
        }

        // 物理防御適用 (高耐久用計算式)
        double finalDamage = applyDefense(incomingDamage, defenderStats.getFinal(StatType.DEFENSE), HEAVY_DEFENSE_DIVISOR);

        // ★★★ 盾の処理 (PvE) ★★★
        // プレイヤーが盾を構えている場合、ここでも軽減ロジックを適用
        if (player.isBlocking() && attacker != null) {
            Vector attackerDir = attacker.getLocation().toVector().subtract(player.getLocation().toVector()).normalize();
            Vector playerDir = player.getLocation().getDirection();

            if (attackerDir.dot(playerDir) < 0) { // 正面
                double blockRate = defenderStats.getFinal(StatType.SHIELD_BLOCK_RATE);
                blockRate = Math.max(0.0, Math.min(blockRate, 1.0));

                double blocked = finalDamage * blockRate;
                finalDamage -= blocked;

                player.getWorld().playSound(player.getLocation(), Sound.ITEM_SHIELD_BLOCK, 1f, 1f);
                player.sendMessage("§b盾防御(PvE)！ §7軽減: §a" + Math.round(blocked));
            }
        }

        e.setDamage(0.0);
        processPlayerDamageWithAbsorption(player, finalDamage, attacker != null ? attacker.getName() : "環境");
    }

    // ----------------------------------------------------
    // --- Helper Methods (最適化・共通化) ---
    // ----------------------------------------------------

    private StatMap getDefenderStats(LivingEntity entity) {
        if (entity instanceof Player p) {
            return StatManager.getTotalStatsFromEquipment(p);
        }
        return new StatMap(); // Mobのステータスが必要ならここで取得
    }

    private double applyDefense(double damage, double defense, double divisor) {
        double reduction = defense / (defense + divisor);
        return damage * (1.0 - reduction);
    }

    private boolean rollChance(double chance) {
        return (Math.random() * 100) + 1 <= chance;
    }

    // Undead特攻ロジック
    private boolean handleUndeadDamage(Player player, LivingEntity target, MythicDamageEvent e) {
        String mobId = MythicBukkit.inst().getMobManager().getActiveMob(target.getUniqueId())
                .map(am -> am.getMobType()).orElse(null);

        if (mobId != null && UNDEAD_MOB_IDS.contains(mobId)) {
            double damage = target.getHealth() * 0.5; // 50%確定ダメージ
            player.sendMessage("§5§l聖特攻！ §fアンデッドに§5§l50%ダメージ！");
            target.getWorld().playSound(target.getLocation(), Sound.BLOCK_ANVIL_LAND, 1.0f, 0.5f);

            e.setDamage(0.0);
            e.setCancelled(true);
            applyCustomDamage(target, damage, player);
            return true;
        }
        return false;
    }

    // Lifestealロジック
    private void handleLifesteal(Player player, LivingEntity target, double baseDamage) {
        double heal = target.getMaxHealth() * (baseDamage / 100.0 / 100.0);
        heal = Math.min(heal, player.getMaxHealth() * 0.20);

        double newHealth = Math.min(player.getHealth() + heal, player.getMaxHealth());
        player.setHealth(newHealth);

        player.sendMessage(String.format("§a§lLS！ §2%.1f §a回復", heal));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 2f);
    }

    // Mobからの特殊ダメージ計算
    private double handleMobDamageLogic(LivingEntity attacker, double damage, Player target) {
        // 弓チェックなどはここに追加
        // クリティカル判定
        if (rollChance(20)) { // 仮: 20%
            damage *= 1.5;
            target.sendMessage("§4§l敵のクリティカル！");
            target.getWorld().playSound(target.getLocation(), Sound.ENTITY_PLAYER_ATTACK_CRIT, 1f, 0.8f);
        }
        return damage;
    }

    // 槍の範囲攻撃
    private void handleSpearCleave(Player attacker, LivingEntity mainTarget, double damage) {
        double cleaveDmg = damage * 0.5;
        Vector dir = attacker.getLocation().getDirection().normalize();
        Location checkLoc = mainTarget.getLocation().clone().add(dir);

        mainTarget.getWorld().getNearbyEntities(checkLoc, 1.5, 1.5, 1.5).stream()
                .filter(e -> e instanceof LivingEntity && e != attacker && e != mainTarget)
                .map(e -> (LivingEntity) e)
                .forEach(target -> {
                    if (!isProcessingDamage.contains(attacker.getUniqueId())) {
                        applyCustomDamage(target, cleaveDmg, attacker);
                        attacker.getWorld().spawnParticle(Particle.SWEEP_ATTACK, target.getLocation().add(0,1,0), 1);
                        attacker.sendMessage("§e貫通！ §c+" + Math.round(cleaveDmg));
                    }
                });
    }

    private void applyCustomDamage(LivingEntity target, double damage, Player damager) {
        if (target instanceof Player p) {
            processPlayerDamageWithAbsorption(p, damage, damager.getName());
        } else {
            isProcessingDamage.add(damager.getUniqueId());
            try {
                target.damage(damage, damager);
            } finally {
                isProcessingDamage.remove(damager.getUniqueId());
            }
        }
    }

    private double applyMultiHitDecay(Player attacker, LivingEntity target, double damage) {
        UUID attackerId = attacker.getUniqueId();
        UUID targetId = target.getUniqueId();
        long now = System.currentTimeMillis();

        magicHitMap.putIfAbsent(attackerId, new HashMap<>());
        Map<UUID, MagicHitInfo> targetMap = magicHitMap.get(attackerId);

        MagicHitInfo info = targetMap.get(targetId);

        // 1秒（1000ms）以内なら連続Hit扱い
        if (info != null && now - info.lastHitTime <= 1000) {
            info.hitCount++;
            info.lastHitTime = now;
        } else {
            info = new MagicHitInfo(1, now);
            targetMap.put(targetId, info);
        }

        // 2Hit目以降のみ減衰
        if (info.hitCount >= 2) {
            double multiplier = Math.pow(0.85, info.hitCount - 1);
            multiplier = Math.max(multiplier, 0.5); // 最低50%
            return damage * multiplier;
        }

        return damage;
    }


    // HP圧縮 + 衝撃吸収処理
    private void processPlayerDamageWithAbsorption(Player player, double damage, String sourceName) {
        double absorption = player.getAbsorptionAmount() * 10.0; // カスタム換算

        if (absorption > 0) {
            if (damage <= absorption) {
                double newAbs = absorption - damage;
                player.setAbsorptionAmount((float)(newAbs / 10.0));
                player.sendMessage("§eシールド防御: -" + Math.round(damage));
                return;
            } else {
                player.setAbsorptionAmount(0f);
                player.sendMessage("§cシールドブレイク！");
                // 残りダメージは無効化(仕様通り)
                return;
            }
        }

        double currentHp = statManager.getActualCurrentHealth(player);
        double newHp = currentHp - damage;
        statManager.setActualCurrentHealth(player, newHp);

        if (newHp <= 0) player.sendMessage("§4" + sourceName + "に倒されました。");
        else player.sendMessage("§c-" + Math.round(damage) + " HP");
    }


    // ----------------------------------------------------
    // --- E. ユーティリティメソッド (既存のロジックから抽出) ---
    // ----------------------------------------------------

    // 既存の距離倍率計算ロジックを独立したメソッドとして抽出
    private double calculateDistanceMultiplier(Player player, LivingEntity targetLiving) {
        double distance = targetLiving.getLocation().distance(player.getLocation());

        final double MIN_DISTANCE = 10.0;
        final double MAX_BOOST_DISTANCE = 40.0;
        final double MAX_MULTIPLIER = 1.2;
        final double MIN_MULTIPLIER = 0.6;

        double distanceMultiplier;

        if (distance <= MIN_DISTANCE) {
            double range = MIN_DISTANCE;
            double minMaxDiff = 1.0 - MIN_MULTIPLIER;
            distanceMultiplier = MIN_MULTIPLIER + (distance / range) * minMaxDiff;

        } else if (distance >= MAX_BOOST_DISTANCE) {
            distanceMultiplier = MAX_MULTIPLIER;

        } else {
            double range = MAX_BOOST_DISTANCE - MIN_DISTANCE;
            double current = distance - MIN_DISTANCE;
            double minMaxDiff = MAX_MULTIPLIER - 1.0;
            distanceMultiplier = 1.0 + (current / range) * minMaxDiff;
        }

        return Math.max(MIN_MULTIPLIER, Math.min(distanceMultiplier, MAX_MULTIPLIER));
    }

    // ----------------------------------------------------
    // ★ ヘルパーメソッド: 槍判定
    // ----------------------------------------------------
    private boolean isSpearWeapon(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                // "タイプ: 槍" をチェック
                if (line.contains("§7カテゴリ:§f槍")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * WorldGuardのPvPフラグをチェックし、PvPが拒否されている場合はtrueを返す。
     * @param attacker 攻撃者 (Player)
     * @param target ターゲット (LivingEntity)
     * @return PvPが許可されていない場合 (false) は true を返す
     */
    private boolean isPvPPrevented(Player attacker, LivingEntity target) {
        if (!target.getWorld().getName().equals(attacker.getWorld().getName())) {
            return false; // 異なるワールド間の攻撃は通常許可される（要件次第）
        }

        // 攻撃者がプレイヤーでない場合、このチェックは不要だが、念のためガード
        if (!(target instanceof Player)) {
            return false;
        }

        // WorldGuardが有効でなければ何もしない
        if (!org.bukkit.Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) {
            return false;
        }

        try {
            com.sk89q.worldguard.protection.regions.RegionContainer container = com.sk89q.worldguard.WorldGuard.getInstance().getPlatform().getRegionContainer();
            com.sk89q.worldguard.protection.regions.RegionQuery query = container.createQuery();

            // 攻撃者の位置でPvPフラグをチェック
            com.sk89q.worldguard.protection.ApplicableRegionSet set = query.getApplicableRegions(com.sk89q.worldedit.bukkit.BukkitAdapter.adapt(attacker.getLocation()));

            // PvPフラグがDENYされているかを確認
            // target=nullでチェックすると、グローバルやリージョン内のフラグが適用される
            if (!set.testState(null, com.sk89q.worldguard.protection.flags.Flags.PVP)) {
                // PvPがDENYされている
                attacker.sendMessage("§cこの区域ではPvPが禁止されています。");
                return true; // ダメージ適用を防止
            }
            return false;

        } catch (NoClassDefFoundError ex) {
            // WorldGuardが見つからない場合の例外処理
            return false;
        }
    }

    /**
     * On-Hitスキル発動ロジック
     * @param attacker スキルを発動するプレイヤー
     * @param target スキルターゲット
     * @param item 攻撃に使用したアイテム
     */
    private void tryTriggerOnHitSkill(Player attacker, LivingEntity target, ItemStack item) {
        if (item == null || !item.hasItemMeta()) return;

        ItemMeta meta = item.getItemMeta();
        PersistentDataContainer container = meta.getPersistentDataContainer();

        // 1. PDCから設定値を読み込み
        Double chance = container.get(ItemLoader.SKILL_CHANCE_KEY, PersistentDataType.DOUBLE);
        Integer cooldown = container.get(ItemLoader.SKILL_COOLDOWN_KEY, PersistentDataType.INTEGER);
        String skillId = container.get(ItemLoader.SKILL_ID_KEY, PersistentDataType.STRING);

        if (chance == null || skillId == null) return;

        // 2. クールダウンチェック
        long lastTrigger = onHitCooldowns.getOrDefault(attacker.getUniqueId(), 0L);
        long currentTime = System.currentTimeMillis();
        long cooldownMillis = (cooldown != null) ? cooldown * 1000L : 0L;

        if (currentTime < lastTrigger + cooldownMillis) {
            // クールダウン中
            // attacker.sendMessage("§e[On-Hit] クールダウン中: あと " + (lastTrigger + cooldownMillis - currentTime) / 1000.0 + "秒"); // デバッグ用
            return;
        }

        // 3. 確率判定 (1d100)
        int roll = (int) (Math.random() * 100) + 1;

        if (roll <= chance) {
            // 4. スキル発動
            MythicBukkit.inst().getAPIHelper().castSkill(attacker.getPlayer(),skillId);

            // 5. クールダウンを更新
            onHitCooldowns.put(attacker.getUniqueId(), currentTime);

            // フィードバック
            attacker.sendMessage("§a[On-Hit] スキル「§b" + skillId + "§a」を発動！");
        }
    }

    // バニラの回復フック
    @EventHandler
    public void onRegain(EntityRegainHealthEvent e) {
        if (e.getEntity() instanceof Player p && !e.isCancelled()) {
            double amount = e.getAmount();
            e.setCancelled(true);
            statManager.healCustomHealth(p, amount);
        }
    }
}
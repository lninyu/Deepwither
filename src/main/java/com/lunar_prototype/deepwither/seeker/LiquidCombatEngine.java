package com.lunar_prototype.deepwither.seeker;

import java.util.Comparator;

public class LiquidCombatEngine {

    /**
     * コンテキストと脳の状態を受け取り、意思決定を行う
     */
    public BanditDecision think(BanditContext context, LiquidBrain brain) {
        // --- 1. 環境情報の正規化 (入力シグナルの作成) ---

        // HP状況 (低いほど1.0に近い)
        double hpStress = 1.0 - (context.entity.hp_pct / 100.0);

        // 敵との距離情報の解析
        double enemyDist = 20.0; // デフォルト遠方
        boolean hasSight = false;

        if (!context.environment.nearby_enemies.isEmpty()) {
            BanditContext.EnemyInfo nearest = context.environment.nearby_enemies.stream()
                    .min(Comparator.comparingDouble(e -> e.dist)).orElse(null);
            if (nearest != null) {
                enemyDist = nearest.dist;
                hasSight = nearest.in_sight;
            }
        }

        // 敵が接近しているか？ (距離の微分的要素)
        boolean enemyClosingIn = false;
        if (brain.lastEnemyDist >= 0 && enemyDist < brain.lastEnemyDist) {
            enemyClosingIn = true;
        }
        brain.lastEnemyDist = enemyDist;

        // --- 2. 緊迫度 (Urgency) の計算 ---
        // これが「液体の粘性」を決定する。
        // HPが低い、または敵が至近距離(5m以内)にいる場合、思考は流動的(即応的)になる。
        double urgency = 0.0;
        if (hpStress > 0.7) urgency += 0.5;
        if (enemyDist < 5.0) urgency += 0.4;
        if (enemyClosingIn) urgency += 0.2;
        urgency = Math.min(1.0, urgency);

        // --- 3. ニューロンの動的更新 (Liquid Update) ---

        // [攻撃性] 敵が見えていて、かつHPに余裕があれば上がる。敵が近づくと急上昇。
        double aggressionInput = (hasSight ? 0.6 : 0.0) + (1.0 - hpStress) * 0.4;
        if (enemyDist < 8.0) aggressionInput += 0.3;
        brain.aggression.update(aggressionInput, urgency);

        // [恐怖] HPが減る、または敵が強そう(High HP)だと上がる。
        double fearInput = hpStress;
        // 敵のHPが高い(high)と認識している場合、恐怖入力増
        if (!context.environment.nearby_enemies.isEmpty() &&
                "high".equals(context.environment.nearby_enemies.get(0).health)) {
            fearInput += 0.3;
        }
        brain.fear.update(fearInput, urgency);

        // [戦術] 恐怖と攻撃のバランス、または遮蔽物が近くにある場合に刺激される
        double coverAvail = (context.environment.nearest_cover != null) ? 0.8 : 0.0;
        // 撃ち合い(中距離)でこそ戦術が必要
        double tacticalInput = (enemyDist > 5.0 && enemyDist < 15.0) ? 0.8 : 0.0;
        tacticalInput += coverAvail * 0.5;
        brain.tactical.update(tacticalInput, urgency * 0.5); // 戦術思考は少し冷静に

        // --- 4. 意思決定 (Actuator) ---
        return resolveDecision(brain, context);
    }

    private BanditDecision resolveDecision(LiquidBrain brain, BanditContext context) {
        BanditDecision d = new BanditDecision();
        d.decision = new BanditDecision.DecisionCore();
        d.movement = new BanditDecision.MovementPlan();
        d.communication = new BanditDecision.Communication();

        double agg = brain.aggression.get();
        double fear = brain.fear.get();
        double tac = brain.tactical.get();

        // 意思決定の理由をデバッグ用に記録
        d.reasoning = String.format("A:%.2f F:%.2f T:%.2f", agg, fear, tac);

        if (fear > agg && fear > 0.6) {
            // [逃走/防御モード]
            d.decision.action_type = "RETREAT";
            d.decision.new_stance = "DEFENSIVE";
            d.movement.destination = "NEAREST_COVER";

            // 恐怖が極限かつHPが低い場合、助けを呼ぶ
            if (fear > 0.8 && context.entity.hp_pct < 20) {
                d.communication.voice_line = "I'm gonna die! Help!";
                d.communication.shout_to_allies = "COVER_ME";
            } else {
                d.communication.voice_line = "Falling back!";
            }

        } else if (tac > agg && tac > 0.4) {
            // [戦術モード] 遮蔽を使って戦う
            d.decision.action_type = "TACTICAL";
            d.decision.new_stance = "DEFAULT";

            // すでに遮蔽に近ければ攻撃、そうでなければ遮蔽へ
            if (context.environment.nearest_cover != null && context.environment.nearest_cover.dist < 2.0) {
                d.movement.destination = "ENEMY"; // 遮蔽から撃つイメージ
            } else {
                d.movement.destination = "NEAREST_COVER";
            }
            d.communication.voice_line = "Taking position.";

        } else {
            // [攻撃モード] デフォルトあるいは攻撃性が高い
            d.decision.action_type = "ATTACK";
            d.decision.new_stance = "AGGRESSIVE";
            d.movement.destination = "ENEMY";

            // 攻撃性が非常に高い場合、スキル使用
            if (agg > 0.75) {
                d.decision.use_skill = "HeavySmash"; // 例
                d.communication.voice_line = "Die!";
            }
        }

        return d;
    }
}
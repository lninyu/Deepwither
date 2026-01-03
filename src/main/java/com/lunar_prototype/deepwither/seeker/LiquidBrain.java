package com.lunar_prototype.deepwither.seeker;

public class LiquidBrain {
    // 思考状態を表すニューロン群
    public final LiquidNeuron aggression; // 攻撃性
    public final LiquidNeuron fear;       // 恐怖/慎重さ
    public final LiquidNeuron tactical;   // 戦術的思考 (遮蔽物利用など)

    // 最後に確認した敵との距離 (変化量検出用)
    public double lastEnemyDist = -1.0;

    public LiquidBrain() {
        // aggressionは冷めにくい (baseDecay低め)
        this.aggression = new LiquidNeuron(0.05);
        // fearは反応しやすい (baseDecay高め)
        this.fear = new LiquidNeuron(0.1);
        // tacticalは中庸
        this.tactical = new LiquidNeuron(0.08);
    }
}
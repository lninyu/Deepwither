package com.lunar_prototype.deepwither.dungeon;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BlockTypes;

public class DungeonPart {
    private final String fileName;
    private final String type;
    private final int length;

    // マーカーの相対座標
    private BlockVector3 entryOffset;
    private BlockVector3 exitOffset;

    public DungeonPart(String fileName, String type, int length) {
        this.fileName = fileName;
        this.type = type;
        this.length = length;
    }

    /**
     * Schematic内のマーカー(金・鉄)をスキャンして記録する
     */
    public void scanMarkers(Clipboard clipboard) {
        BlockVector3 origin = clipboard.getOrigin(); // コピーした時の立ち位置

        for (BlockVector3 pos : clipboard.getRegion()) {
            var block = clipboard.getFullBlock(pos);

            // コピー基準点(Origin)からの相対座標を記録する
            if (block.getBlockType().equals(BlockTypes.GOLD_BLOCK)) {
                this.entryOffset = pos.subtract(origin);
            } else if (block.getBlockType().equals(BlockTypes.IRON_BLOCK)) {
                this.exitOffset = pos.subtract(origin);
            }
        }
    }

    /**
     * 回転後の「入口」オフセットを取得 (WorldEdit純正の計算を使用)
     */
    public BlockVector3 getRotatedEntryOffset(int rotation) {
        return transformVector(entryOffset, rotation);
    }

    /**
     * 回転後の「出口」オフセットを取得
     */
    public BlockVector3 getRotatedExitOffset(int rotation) {
        return transformVector(exitOffset, rotation);
    }

    /**
     * AffineTransformを使ってベクトルを正確に回転させる
     */
    private BlockVector3 transformVector(BlockVector3 vec, int angle) {
        if (vec == null) return BlockVector3.at(0, 0, 0);
        if (angle == 0) return vec;

        // Y軸周りの回転行列を作成 (角度は度数法)
        AffineTransform transform = new AffineTransform().rotateY(angle);

        // 1. BlockVector3 を Vector3 (浮動小数点) に変換
        // 2. transform.apply で回転を適用
        // 3. 結果を BlockVector3 (整数) に変換
        com.sk89q.worldedit.math.Vector3 result = transform.apply(vec.toVector3());

        // 四捨五入を考慮して整数ベクトルに変換
        return BlockVector3.at(
                Math.round(result.getX()),
                Math.round(result.getY()),
                Math.round(result.getZ())
        );
    }

    public String getFileName() { return fileName; }
    public String getType() { return type; }
    public int getLength() { return length; }
}
package com.lunar_prototype.deepwither.dungeon;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BlockTypes;

public class DungeonPart {
    private final String fileName;
    private final String type;
    private final int length;

    // マーカーの「生座標」(Schematic内の絶対座標)
    private BlockVector3 entryPos;

    // 入口から出口までのベクトル（相対距離）
    private BlockVector3 exitVector;

    public DungeonPart(String fileName, String type, int length) {
        this.fileName = fileName;
        this.type = type;
        this.length = length;
    }

    public void scanMarkers(Clipboard clipboard) {
        BlockVector3 tempEntry = null;
        BlockVector3 tempExit = null;

        // Origin（原点）を引き算せず、座標をそのまま取得する
        for (BlockVector3 pos : clipboard.getRegion()) {
            var block = clipboard.getFullBlock(pos);

            if (block.getBlockType().equals(BlockTypes.GOLD_BLOCK)) {
                tempEntry = pos;
            } else if (block.getBlockType().equals(BlockTypes.IRON_BLOCK)) {
                tempExit = pos;
            }
        }

        // 座標が見つかった場合の処理
        if (tempEntry != null) {
            this.entryPos = tempEntry;

            // 出口も見つかっていれば、「入口から出口へのベクトル」を計算
            // これなら座標系がどうなっていても「ブロック間の距離」なので正確です
            if (tempExit != null) {
                this.exitVector = tempExit.subtract(tempEntry);
            } else {
                this.exitVector = BlockVector3.ZERO;
            }
        } else {
            // 入口がない場合は便宜上 (0,0,0) を使うなどのフォールバック
            this.entryPos = clipboard.getOrigin();
            this.exitVector = BlockVector3.ZERO;
        }
    }

    /**
     * ロードしたClipboardのOriginを、強制的に入口位置に合わせるための座標を返す
     */
    public BlockVector3 getEntryPos() {
        return entryPos;
    }

    /**
     * 回転後の「出口までのベクトル」を取得
     */
    public BlockVector3 getRotatedExitVector(int rotation) {
        return transformVector(exitVector, rotation);
    }

    private BlockVector3 transformVector(BlockVector3 vec, int angle) {
        if (vec == null) return BlockVector3.ZERO;
        if (angle == 0) return vec;

        // 回転行列を作成
        AffineTransform transform = new AffineTransform().rotateY(angle);

        // 1. BlockVector3 -> Vector3 (浮動小数点) に変換して回転適用
        com.sk89q.worldedit.math.Vector3 result = transform.apply(vec.toVector3());

        // 2. 結果を四捨五入して BlockVector3 (整数) に戻す
        // toBlockVector3() が使えない環境でも、at() は必ず使えます
        return BlockVector3.at(
                Math.round(result.getX()),
                Math.round(result.getY()),
                Math.round(result.getZ())
        );
    }

    public String getFileName() { return fileName; }
    public String getType() { return type; }
}
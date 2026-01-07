package com.lunar_prototype.deepwither.dungeon;

import com.lunar_prototype.deepwither.Deepwither;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.math.transform.AffineTransform;
import com.sk89q.worldedit.world.block.BlockTypes;

public class DungeonPart {
    private final String fileName;
    private final String type;
    private final int length;

    // Origin(保存時の立ち位置)からの相対座標
    private BlockVector3 entryOffset = BlockVector3.ZERO;
    private BlockVector3 exitOffset = BlockVector3.ZERO;

    public DungeonPart(String fileName, String type, int length) {
        this.fileName = fileName;
        this.type = type;
        this.length = length;
    }

    public void scanMarkers(Clipboard clipboard) {
        BlockVector3 origin = clipboard.getOrigin();
        boolean foundEntry = false;
        boolean foundExit = false;

        Deepwither.getInstance().getLogger().info("[" + fileName + "] Scanning markers... Origin: " + origin);

        for (BlockVector3 pos : clipboard.getRegion()) {
            var block = clipboard.getFullBlock(pos);

            // 金ブロック (入口)
            if (block.getBlockType().equals(BlockTypes.GOLD_BLOCK)) {
                this.entryOffset = pos.subtract(origin);
                foundEntry = true;
                Deepwither.getInstance().getLogger().info("  -> Found ENTRY (Gold). RelOffset: " + entryOffset);
            }
            // 鉄ブロック (出口)
            else if (block.getBlockType().equals(BlockTypes.IRON_BLOCK)) {
                this.exitOffset = pos.subtract(origin);
                foundExit = true;
                Deepwither.getInstance().getLogger().info("  -> Found EXIT (Iron). RelOffset: " + exitOffset);
            }
        }

        if (!foundEntry) {
            Deepwither.getInstance().getLogger().warning("  [!] WARNING: No ENTRY (Gold Block) found in " + fileName + ". Using (0,0,0).");
        }
        if (!foundExit && !type.equals("ROOM")) { // ROOMタイプ等は出口がなくてもいいかも
            Deepwither.getInstance().getLogger().warning("  [!] WARNING: No EXIT (Iron Block) found in " + fileName + ". Using (0,0,0).");
        }
    }

    /**
     * 回転後の「入口」オフセットを取得
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
     * Y軸周りにベクトルを回転させる（90度単位専用）
     * WorldEditのAPIを使わず、単純な座標入れ替えで計算するためバグらない
     */
    private BlockVector3 transformVector(BlockVector3 vec, int angle) {
        if (vec == null) return BlockVector3.ZERO;

        // 負の角度を正の角度(0, 90, 180, 270)に正規化
        int normalizedAngle = angle % 360;
        if (normalizedAngle < 0) normalizedAngle += 360;

        int x = vec.getX();
        int y = vec.getY();
        int z = vec.getZ();

        switch (normalizedAngle) {
            case 90:
                // 90度回転: (x, z) -> (-z, x)
                return BlockVector3.at(-z, y, x);
            case 180:
                // 180度回転: (x, z) -> (-x, -z)
                return BlockVector3.at(-x, y, -z);
            case 270:
                // 270度回転: (x, z) -> (z, -x)
                return BlockVector3.at(z, y, -x);
            case 0:
            default:
                // 0度（そのまま）
                return vec;
        }
    }

    public String getFileName() { return fileName; }
    public String getType() { return type; }
}
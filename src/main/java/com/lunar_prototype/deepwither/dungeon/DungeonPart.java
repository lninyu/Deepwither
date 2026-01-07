package com.lunar_prototype.deepwither.dungeon;

import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldedit.world.block.BlockTypes;

public class DungeonPart {
    private final String fileName;
    private final String type;
    private final int length;

    private BlockVector3 entryOffset;
    private BlockVector3 exitOffset;

    public DungeonPart(String fileName, String type, int length) {
        this.fileName = fileName;
        this.type = type;
        this.length = length;
    }

    // --- 回転を考慮した座標取得 ---

    /**
     * 指定された回転角(0, 90, 180, 270)に基づいた入口オフセットを返します。
     */
    public BlockVector3 getRotatedEntryOffset(int rotation) {
        return rotateVector(entryOffset, rotation);
    }

    /**
     * 指定された回転角に基づいた出口オフセットを返します。
     */
    public BlockVector3 getRotatedExitOffset(int rotation) {
        return rotateVector(exitOffset, rotation);
    }

    /**
     * ベクトルをY軸を中心に回転させます。
     * (x, z) -> (z, -x) [90度] のような回転行列計算
     */
    private BlockVector3 rotateVector(BlockVector3 vec, int angle) {
        if (vec == null) return null;
        int x = vec.getX();
        int y = vec.getY();
        int z = vec.getZ();

        return switch (angle % 360) {
            case 90 -> BlockVector3.at(-z, y, x);
            case 180 -> BlockVector3.at(-x, y, -z);
            case 270 -> BlockVector3.at(z, y, -x);
            default -> vec; // 0度
        };
    }

    // scanMarkers メソッドは以前のまま
    public void scanMarkers(Clipboard clipboard) {
        BlockVector3 origin = clipboard.getOrigin();
        for (BlockVector3 pos : clipboard.getRegion()) {
            var block = clipboard.getFullBlock(pos);
            if (block.getBlockType().equals(BlockTypes.GOLD_BLOCK)) {
                this.entryOffset = pos.subtract(origin);
            } else if (block.getBlockType().equals(BlockTypes.IRON_BLOCK)) {
                this.exitOffset = pos.subtract(origin);
            }
        }
    }

    public String getFileName() { return fileName; }
    public String getType() { return type; }
}
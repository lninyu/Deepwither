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

    // Origin(Schematic保存時の立ち位置)からの相対座標
    // private BlockVector3 entryOffset = BlockVector3.ZERO;
    // private BlockVector3 exitOffset = BlockVector3.ZERO;

    // Store as primitives to avoid vector object weirdness/mutability issues during
    // debug
    private int entryX, entryY, entryZ;
    private int exitX, exitY, exitZ;

    public DungeonPart(String fileName, String type, int length) {
        this.fileName = fileName;
        this.type = type;
        this.length = length;
    }

    public void scanMarkers(Clipboard clipboard) {
        BlockVector3 origin = clipboard.getOrigin();
        Deepwither.getInstance().getLogger().info(String.format("[%s] Scanning Part (ID:%d). Origin:%s",
                fileName, System.identityHashCode(this), origin));

        boolean foundEntry = false;
        boolean foundExit = false;

        for (BlockVector3 pos : clipboard.getRegion()) {
            var block = clipboard.getFullBlock(pos);

            // 金ブロック (入口) -> 接続元を受け入れる場所
            if (block.getBlockType().equals(BlockTypes.GOLD_BLOCK)) {
                // this.entryOffset = pos.subtract(origin);
                this.entryX = pos.getX() - origin.getX();
                this.entryY = pos.getY() - origin.getY();
                this.entryZ = pos.getZ() - origin.getZ();

                Deepwither.getInstance().getLogger().info(String.format(
                        "[%s] Found ENTRY(Gold). Pos:%s - Origin:%s = %d,%d,%d",
                        fileName, pos, origin, entryX, entryY, entryZ));
                foundEntry = true;
            }
            // 鉄ブロック (出口) -> 次のパーツへ接続する場所
            else if (block.getBlockType().equals(BlockTypes.IRON_BLOCK)) {
                // this.exitOffset = pos.subtract(origin);
                this.exitX = pos.getX() - origin.getX();
                this.exitY = pos.getY() - origin.getY();
                this.exitZ = pos.getZ() - origin.getZ();

                Deepwither.getInstance().getLogger().info(String.format(
                        "[%s] Found EXIT(Iron). Pos:%s - Origin:%s = %d,%d,%d",
                        fileName, pos, origin, exitX, exitY, exitZ));
                foundExit = true;
            }
        }

        if (!foundEntry) {
            Deepwither.getInstance().getLogger()
                    .warning("[" + fileName + "] Warning: No Gold Block (Entry) found. Assuming (0,0,0).");
        }
        // ROOMや終端用パーツの場合、出口がないことは正常なのでWarningを出さない判定にしても良い
        if (!foundExit && !type.equals("ROOM")) {
            Deepwither.getInstance().getLogger()
                    .info("[" + fileName + "] Info: No Iron Block (Exit) found. (Normal for dead-ends)");
        }
    }

    /**
     * 回転後の「入口」オフセットを取得
     */
    public BlockVector3 getRotatedEntryOffset(int rotation) {
        return transformVector(getEntryOffset(), rotation);
    }

    /**
     * 回転後の「出口」オフセットを取得
     */
    public BlockVector3 getRotatedExitOffset(int rotation) {
        return transformVector(getExitOffset(), rotation);
    }

    /**
     * Y軸周りの回転 (時計回り)
     * WEのrotateYは反時計回り等の場合があるため、動作確認して逆になるようなら符号を反転させてください。
     * ここでは一般的な座標回転行列に基づいています。
     */
    private BlockVector3 transformVector(BlockVector3 vec, int angle) {
        if (vec == null)
            return BlockVector3.ZERO;

        int normalizedAngle = angle % 360;
        if (normalizedAngle < 0)
            normalizedAngle += 360;

        // WorldEditの回転仕様に合わせて調整 (通常: 時計回り)
        // Use WorldEdit's AffineTransform to ensure consistent rotation logic with
        // clipboard operations
        AffineTransform transform = new AffineTransform().rotateY(normalizedAngle);
        var v3 = transform.apply(vec.toVector3());
        return BlockVector3.at(v3.getX(), v3.getY(), v3.getZ());
    }

    public BlockVector3 getEntryOffset() {
        return BlockVector3.at(entryX, entryY, entryZ);
    }

    public BlockVector3 getExitOffset() {
        return BlockVector3.at(exitX, exitY, exitZ);
    }

    public String getFileName() {
        return fileName;
    }

    public String getType() {
        return type;
    }
}
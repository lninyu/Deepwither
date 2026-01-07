package com.lunar_prototype.deepwither.dungeon;

import com.lunar_prototype.deepwither.Deepwither;
import com.sk89q.worldedit.extent.clipboard.Clipboard;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormat;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats;
import com.sk89q.worldedit.extent.clipboard.io.ClipboardReader;
import com.sk89q.worldedit.math.BlockVector3;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DungeonGenerator {
    private final String dungeonName;
    private final List<DungeonPart> partList = new ArrayList<>();
    private final File dungeonFolder;

    public DungeonGenerator(String dungeonName) {
        this.dungeonName = dungeonName;
        this.dungeonFolder = new File(Deepwither.getInstance().getDataFolder(), "dungeons/" + dungeonName);
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File(Deepwither.getInstance().getDataFolder(), "dungeons/" + dungeonName + ".yml");
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);

        List<Map<?, ?>> maps = config.getMapList("parts");

        for (Map<?, ?> rawMap : maps) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) rawMap;

            int length = ((Number) map.getOrDefault("length", 10)).intValue();
            String fileName = (String) map.get("file");
            String type = (String) map.get("type");

            if (fileName != null && type != null) {
                DungeonPart part = new DungeonPart(fileName, type.toUpperCase(), length);

                // --- 追加: マーカーのスキャン ---
                File schemFile = new File(dungeonFolder, fileName);
                if (schemFile.exists()) {
                    scanPartMarkers(part, schemFile);
                }

                partList.add(part);
            }
        }
    }

    /**
     * Schematicファイルを一時的に読み込んでマーカーをスキャンする
     */
    private void scanPartMarkers(DungeonPart part, File file) {
        ClipboardFormat format = ClipboardFormats.findByFile(file);
        if (format == null) return;

        try (ClipboardReader reader = format.getReader(new FileInputStream(file))) {
            Clipboard clipboard = reader.read();
            part.scanMarkers(clipboard); // 前のステップで作ったscanMarkersを呼び出し
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 回転を考慮して一直線のダンジョンを生成する
     */
    public void generateStraight(World world, int hallwayCount, int rotation) {
        Location nextAnchor = new Location(world, 0, 64, 0);

        // 1. Entrance
        DungeonPart entrance = findPartByType("ENTRANCE");
        if (entrance != null) {
            nextAnchor = pasteAndGetNextAnchor(nextAnchor, entrance, rotation);
        }

        // 2. Hallways
        DungeonPart hallway = findPartByType("HALLWAY");
        if (hallway != null) {
            for (int i = 0; i < hallwayCount; i++) {
                nextAnchor = pasteAndGetNextAnchor(nextAnchor, hallway, rotation);
            }
        }
    }

    private Location pasteAndGetNextAnchor(Location anchor, DungeonPart part, int rotation) {
        File file = new File(dungeonFolder, part.getFileName());

        // 回転後のオフセットを取得
        BlockVector3 rotatedEntry = part.getRotatedEntryOffset(rotation);
        BlockVector3 rotatedExit = part.getRotatedExitOffset(rotation);

        // 貼り付け位置(Origin)の計算
        Location pasteLoc = anchor.clone();
        if (rotatedEntry != null) {
            pasteLoc.subtract(rotatedEntry.getX(), rotatedEntry.getY(), rotatedEntry.getZ());
        }

        // FAWEで回転を指定して貼り付け
        SchematicUtil.paste(pasteLoc, file, rotation);

        // 次のアンカーを計算
        if (rotatedExit != null) {
            return pasteLoc.clone().add(rotatedExit.getX(), rotatedExit.getY(), rotatedExit.getZ());
        }
        return anchor.clone().add(0, 0, 10); // fallback
    }

    private DungeonPart findPartByType(String type) {
        return partList.stream().filter(p -> p.getType().equals(type)).findFirst().orElse(null);
    }
}
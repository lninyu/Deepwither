package com.lunar_prototype.deepwither;

import org.bukkit.entity.Player;

import java.io.File;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LevelManager {
    private static final int MAX_LEVEL = 50;

    private final Map<UUID, PlayerLevelData> dataMap = new HashMap<>();
    private final Connection connection;

    public LevelManager(File dbFile) throws SQLException {
        connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS player_levels (
                    uuid TEXT PRIMARY KEY,
                    level INTEGER,
                    exp REAL
                )
            """);
        }
    }

    public void load(UUID uuid) {
        try (PreparedStatement ps = connection.prepareStatement("SELECT level, exp FROM player_levels WHERE uuid = ?")) {
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                int level = Math.min(rs.getInt("level"), MAX_LEVEL);
                double exp = rs.getDouble("exp");
                dataMap.put(uuid, new PlayerLevelData(level, exp));
            } else {
                dataMap.put(uuid, new PlayerLevelData(1, 0));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void save(UUID uuid) {
        PlayerLevelData data = dataMap.get(uuid);
        if (data == null) return;

        int level = Math.min(data.getLevel(), MAX_LEVEL);
        double exp = (level >= MAX_LEVEL) ? 0 : data.getExp(); // 上限ならEXPを0に

        try (PreparedStatement ps = connection.prepareStatement("""
            INSERT INTO player_levels (uuid, level, exp) VALUES (?, ?, ?)
            ON CONFLICT(uuid) DO UPDATE SET level = excluded.level, exp = excluded.exp
        """)) {
            ps.setString(1, uuid.toString());
            ps.setInt(2, level);
            ps.setDouble(3, exp);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addExp(Player player, double amount) {
        PlayerLevelData data = dataMap.get(player.getUniqueId());
        if (data == null || data.getLevel() >= MAX_LEVEL) return;

        int before = data.getLevel();
        data.addExp(amount);
        int after = data.getLevel();

        player.sendMessage("§a+ " + amount + " EXP");

        if (after > before) {
            player.sendMessage("§6Level Up! §e" + before + " → " + after);
            Deepwither.getInstance().getAttributeManager().givePoints(player.getUniqueId(),2);
            UUID uuid = player.getUniqueId();
            SkilltreeManager.SkillData skilldata = Deepwither.getInstance().getSkilltreeManager().load(uuid);
            if (skilldata != null) {
                skilldata.setSkillPoint(skilldata.getSkillPoint() + 2);
                Deepwither.getInstance().getSkilltreeManager().save(uuid, skilldata);
            }
        }

        if (after >= MAX_LEVEL) {
            player.sendMessage("§b最大レベルに到達しました！");
        }
    }

    public void updatePlayerDisplay(Player player) {
        PlayerLevelData data = dataMap.get(player.getUniqueId());

        // 例: 現在のカスタムレベルと進捗データを取得 (あなたの実装に依存)
        int currentLevel = data.getLevel();
        double currentExp = data.getExp();
        double expToNextLevel = data.getRequiredExp();

        // ★ 経験値レベル表示を「現在レベル」に設定
        player.setLevel(currentLevel);

        // ★ 経験値バーを「レベルの進捗」に設定
        // 0.0 (空) から 1.0 (満タン) の間で計算
        float progress = (float) (currentExp / expToNextLevel);
        player.setExp(progress);
    }

    public PlayerLevelData get(Player player) {
        return dataMap.get(player.getUniqueId());
    }

    public void unload(UUID uuid) {
        save(uuid);
        dataMap.remove(uuid);
    }
}
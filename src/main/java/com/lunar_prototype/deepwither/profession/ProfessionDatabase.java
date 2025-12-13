package com.lunar_prototype.deepwither.profession;

import com.lunar_prototype.deepwither.Deepwither;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

public class ProfessionDatabase {

    private final Deepwither plugin;
    private Connection connection;

    public ProfessionDatabase(Deepwither plugin) {
        this.plugin = plugin;
        initialize();
    }

    private void initialize() {
        File dataFolder = new File(plugin.getDataFolder(), "database");
        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }

        File dbFile = new File(dataFolder, "levels.db");
        try {
            if (!dbFile.exists()) {
                dbFile.createNewFile();
            }

            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath());

            // テーブル作成
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS player_professions (" +
                        "player_id VARCHAR(36) NOT NULL," +
                        "profession_type VARCHAR(32) NOT NULL," +
                        "experience BIGINT DEFAULT 0," +
                        "PRIMARY KEY (player_id, profession_type));");
            }

            plugin.getLogger().info("Connected to SQLite database for Professions.");

        } catch (IOException | SQLException | ClassNotFoundException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not initialize SQLite database!", e);
        }
    }

    /**
     * 接続を閉じる (サーバー停止時に呼ぶ)
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * プレイヤーのデータをロード
     */
    public PlayerProfessionData loadPlayer(UUID playerId) {
        PlayerProfessionData data = new PlayerProfessionData(playerId);

        String query = "SELECT profession_type, experience FROM player_professions WHERE player_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, playerId.toString());

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    try {
                        String typeStr = rs.getString("profession_type");
                        long exp = rs.getLong("experience");

                        ProfessionType type = ProfessionType.valueOf(typeStr);
                        // 直接セットするためのメソッドがないため、addExpで差分計算するか、
                        // PlayerProfessionDataにsetterを用意するのがきれいですが、
                        // ここでは既存のaddExpを利用して初期値を入れます (初期値0なのでそのまま加算でOK)
                        data.addExp(type, exp);

                    } catch (IllegalArgumentException ignored) {
                        // 無効な職業タイプはスキップ
                    }
                }
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load profession data for " + playerId, e);
        }

        return data;
    }

    /**
     * プレイヤーのデータを保存
     */
    public void savePlayer(PlayerProfessionData data) {
        String query = "INSERT OR REPLACE INTO player_professions (player_id, profession_type, experience) VALUES (?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            // トランザクション開始
            connection.setAutoCommit(false);

            for (Map.Entry<ProfessionType, Long> entry : data.getAllExperience().entrySet()) {
                ps.setString(1, data.getPlayerId().toString());
                ps.setString(2, entry.getKey().name());
                ps.setLong(3, entry.getValue());
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
            // 自動コミットに戻す
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save profession data for " + data.getPlayerId(), e);
            try {
                connection.rollback();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }
}
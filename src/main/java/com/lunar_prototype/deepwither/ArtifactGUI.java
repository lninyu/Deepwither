package com.lunar_prototype.deepwither;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

public class ArtifactGUI implements Listener {

    private Inventory artifactGUI;
    public static final int[] ARTIFACT_SLOTS = {3, 4, 5};
    // ★ 装飾スロットを定義
    public static final int[] BORDER_SLOTS = {0, 1, 2, 6, 7, 8};

    public ArtifactGUI() {
        // GUIの作成 (9スロット)
        artifactGUI = Bukkit.createInventory(null, 9, "§8[GUI] §6アーティファクト"); // タイトルを見やすく修正

        // 1. アーティファクトスロットのプレースホルダーを準備
        ItemStack artifactPlaceholder = new ItemStack(Material.CYAN_STAINED_GLASS_PANE); // 色を変えて視認性向上
        ItemMeta artifactMeta = artifactPlaceholder.getItemMeta();
        artifactMeta.setDisplayName(ChatColor.AQUA + "【アーティファクトスロット】");
        artifactPlaceholder.setItemMeta(artifactMeta);

        // 2. 装飾用のプレースホルダーを準備
        ItemStack borderPlaceholder = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta borderMeta = borderPlaceholder.getItemMeta();
        borderMeta.setDisplayName("§7- - -"); // 名前を短く
        borderPlaceholder.setItemMeta(borderMeta);

        // 3. ★ 装飾スロットを埋める
        for (int i : BORDER_SLOTS) {
            artifactGUI.setItem(i, borderPlaceholder);
        }

        // 4. アーティファクトスロットのプレースホルダーを配置
        for (int i : ARTIFACT_SLOTS) {
            artifactGUI.setItem(i, artifactPlaceholder);
        }
    }

    public void openArtifactGUI(Player player) {
        // GUIを開く前に、保存されたデータを読み込んでGUIを更新
        updateGUIFromPlayerData(player);
        player.openInventory(this.artifactGUI);
    }

    private void updateGUIFromPlayerData(Player player) {
        // まず、GUIを初期状態に戻す (clearGUIを修正し、ボーダーはそのまま残すようにする)
        // clearGUI(); // clearGUIを廃止し、クリアと初期配置を統合

        // 1. アーティファクトスロットのプレースホルダーを再配置
        ItemStack artifactPlaceholder = new ItemStack(Material.CYAN_STAINED_GLASS_PANE);
        ItemMeta artifactMeta = artifactPlaceholder.getItemMeta();
        artifactMeta.setDisplayName(ChatColor.AQUA + "【アーティファクトスロット】");
        artifactPlaceholder.setItemMeta(artifactMeta);

        // スロットをプレースホルダーでリセット
        for (int i : ARTIFACT_SLOTS) {
            artifactGUI.setItem(i, artifactPlaceholder);
        }

        // 2. 保存されたアーティファクトのリストを取得
        List<ItemStack> savedArtifacts = Deepwither.getInstance().getArtifactManager().getPlayerArtifacts(player);

        // 3. 取得したアイテムをGUIのスロットに配置
        for (int i = 0; i < savedArtifacts.size(); i++) {
            if (i < ARTIFACT_SLOTS.length) {
                // savedArtifactsのアイテムを直接配置 (プレースホルダーを上書き)
                artifactGUI.setItem(ARTIFACT_SLOTS[i], savedArtifacts.get(i));
            }
        }
    }

    public Inventory getInventory() {
        return artifactGUI;
    }
}
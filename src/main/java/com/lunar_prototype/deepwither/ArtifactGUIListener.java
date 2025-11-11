package com.lunar_prototype.deepwither;

import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.List;

public class ArtifactGUIListener implements Listener {

    // GUIのインスタンスを共有するために、ArtifactGUIクラスのインスタンスを渡す
    private ArtifactGUI artifactGUI;
    private StatManager statManager;

    public ArtifactGUIListener(ArtifactGUI gui,StatManager statManager) {
        this.artifactGUI = gui;
        this.statManager = statManager;
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (!event.getView().getTitle().contains("アーティファクト")) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();
        if (clickedInventory == null) {
            return;
        }

        int slot = event.getSlot();

        // **GUI内のクリックを処理**
        if (clickedInventory.equals(event.getView().getTopInventory())) {

            // **ステップ1: クリックされたスロットがアーティファクトスロットかどうかをチェック**
            boolean isArtifactSlot = isArtifactSlot(slot);

            if (isArtifactSlot) {
                // **A. アーティファクトスロット内での操作**

                // **A1. アイテムを持ち出そうとした場合 (現在のアイテム != AIR かつ カーソル = AIR)**
                if (event.getCursor().getType() == Material.AIR && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    if (!isArtifact(event.getCurrentItem())) {
                        // 持ち出そうとしたのがプレースホルダーなどの非アーティファクトアイテムの場合、キャンセル
                        event.setCancelled(true);
                        player.sendMessage("§cこのスロットにあるプレースホルダーは持ち出せません。");
                        return;
                    }
                }

                // **A2. カーソルにアイテムがあり、アーティファクトスロットに置こうとする場合**
                if (event.getCursor().getType() != Material.AIR) {
                    if (!isArtifact(event.getCursor())) {
                        event.setCancelled(true);
                        player.sendMessage("§cこのスロットにはアーティファクトのみ装備できます。");
                    }
                }

            } else {
                // **B. アーティファクトスロット以外のスロット（装飾部分）での操作**
                // 装飾用アイテム（GRAY_STAINED_GLASS_PANEなど）は全て動かせないようにする
                event.setCancelled(true);
                // 念のためメッセージを追加（デバッグ用としても機能）
                player.sendMessage("§c装飾スロットのアイテムは動かせません。");
            }
        }

        // **プレイヤーインベントリ内のクリックを処理**
        if (clickedInventory.equals(player.getInventory())) {
            // **GUIにアイテムを入れようとした場合**
            if (event.getCursor().getType() != Material.AIR) {
                // アーティファクトでなければキャンセル
                if (!isArtifact(event.getCursor())) {
                    if (isArtifactSlot(event.getSlot())) { // 念のためチェック
                        event.setCancelled(true);
                        player.sendMessage("§cこのスロットにはアーティファクトのみ装備できます。");
                    }
                }
            }
        }

        boolean isArtifactSlot = isArtifactSlot(slot);
        if (isArtifactSlot && event.getCursor().getType() != Material.AIR) {
            Bukkit.getScheduler().runTaskLater(Deepwither.getInstance(), () -> {
                // アイテムが正常にスロットに置かれたことを確認
                ItemStack itemInSlot = player.getOpenInventory().getItem(slot);
                if (itemInSlot != null && isArtifact(itemInSlot)) {
                    // カーソルにアイテムが残っている場合、それを消去
                    // （これは入れ替え操作で発生）
                    if (player.getItemOnCursor() != null) {
                        player.setItemOnCursor(new ItemStack(Material.AIR));
                    }
                }
            }, 1L);
        }
    }

    // ArtifactGUIListener.java の onInventoryClose メソッド (修正)

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (event.getView().getTitle().contains("アーティファクト")) { // タイトルチェックを柔軟に
            Player player = (Player) event.getPlayer();
            Inventory guiInventory = event.getInventory();

            List<ItemStack> artifacts = new ArrayList<>();
            for (int slot : ArtifactGUI.ARTIFACT_SLOTS) {
                ItemStack item = guiInventory.getItem(slot);

                // ★ 修正箇所: プレースホルダーは保存しない (CYAN_STAINED_GLASS_PANEの場合)
                if (item != null && item.getType() != Material.CYAN_STAINED_GLASS_PANE) {
                    artifacts.add(item);
                }
            }

            Deepwither.getInstance().getArtifactManager().savePlayerArtifacts(player, artifacts);
            statManager.updatePlayerStats(player);
        }
    }
    /**
     * アーティファクトスロットかどうかを判定
     */
    private boolean isArtifactSlot(int slot) {
        for (int s : ArtifactGUI.ARTIFACT_SLOTS) {
            if (s == slot) {
                return true;
            }
        }
        return false;
    }

    /**
     * 指定されたアイテムがアーティファクトであるかを判定する。
     * @param item 判定対象のアイテム
     * @return アイテムがアーティファクトであればtrue、そうでなければfalse
     */
    private boolean isArtifact(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();
        if (meta.hasLore()) {
            List<String> lore = meta.getLore();
            for (String line : lore) {
                // "アーティファクト"という文字列が含まれているかをチェック
                if (line.contains("アーティファクト")) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * 3つのアーティファクトスロットから統計情報を更新
     */
    private void updateArtifactStats(Player player, Inventory guiInventory) {

        for (int slot : ArtifactGUI.ARTIFACT_SLOTS) {
            ItemStack artifactItem = guiInventory.getItem(slot);
            if (artifactItem != null) {
                // ここでアイテムから統計情報を読み取り、プレイヤーに加算
                // 例: StatManager.addStatsFromItem(player, artifactItem);
            }
        }

        // 全ての装備とアーティファクトの合計統計情報を再計算
        statManager.updatePlayerStats(player);
        double maxMana = StatManager.getTotalStatsFromEquipment(player).getFlat(StatType.MAX_MANA);
        Deepwither.getInstance().getManaManager().get(player.getUniqueId()).setMaxMana(maxMana);
    }
}

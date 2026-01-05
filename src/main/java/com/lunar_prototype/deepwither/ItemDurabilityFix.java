package com.lunar_prototype.deepwither;

import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.inventory.ItemStack;

public class ItemDurabilityFix implements Listener {

    /**
     * アイテムの耐久値がゼロになり破壊される瞬間だけをキャンセルします。
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerItemDamage(PlayerItemDamageEvent e) {
        ItemStack item = e.getItem();
        ItemMeta meta = item.getItemMeta();

        // Damageableインターフェースを実装していないアイテム（耐久値がないもの）は無視
        if (!(meta instanceof Damageable damageable)) {
            return;
        }

        int maxDurability;

        // ★修正ポイント: hasMaxDamage() をチェックして例外を回避
        if (damageable.hasMaxDamage()) {
            // カスタムコンポーネント（max_damage）がある場合はその値を取得
            maxDurability = damageable.getMaxDamage();
        } else {
            // コンポーネントがない場合は、バニラ本来のマテリアル耐久値を取得
            maxDurability = item.getType().getMaxDurability();
        }

        // 耐久値が設定されていないアイテム（ブロックや棒など）は処理を終了
        if (maxDurability <= 0) {
            return;
        }

        // 現在の「受けたダメージ量」を取得
        int currentDamage = damageable.getDamage();
        // 今回追加されるダメージ量
        int damageToApply = e.getDamage();

        // 「現在のダメージ + 今回受けるダメージ」が最大耐久値以上になるかチェック
        if (currentDamage + damageToApply >= maxDurability) {

            // イベントをキャンセルして破壊を防ぐ
            e.setCancelled(true);

            // 耐久値を「最大値 - 1」に固定（壊れる寸前の状態）
            damageable.setDamage(maxDurability - 1);
            item.setItemMeta(damageable);

            // プレイヤーへの通知
            Player player = e.getPlayer();
            String displayName = meta.hasDisplayName() ? meta.getDisplayName() : item.getType().name();

            player.sendMessage(ChatColor.RED + "⚠ " + displayName + " の耐久値が限界です！修理してください。");
            player.playSound(player.getLocation(), Sound.ENTITY_ITEM_BREAK, 0.6f, 0.8f);
            player.playSound(player.getLocation(), Sound.BLOCK_ANVIL_LAND, 0.4f, 1.2f);
        }
    }
}
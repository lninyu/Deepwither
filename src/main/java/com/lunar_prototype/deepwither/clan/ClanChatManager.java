package com.lunar_prototype.deepwither.clan;

import com.lunar_prototype.deepwither.clan.Clan;
import com.lunar_prototype.deepwither.clan.ClanManager;
import com.lunar_prototype.deepwither.util.GoogleImeConverter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ClanChatManager implements Listener {
    private final ClanManager clanManager;

    public ClanChatManager(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String originalMessage = event.getMessage();

        String convertedMessage = GoogleImeConverter.convert(originalMessage);

        Clan clan = clanManager.getClanByPlayer(player.getUniqueId());
        // tag ではなく ID 全文を表示に使用する
        String clanDisplay = (clan != null) ? "§b[" + clan.getId() + "]§r " : "";

        String finalMessage = originalMessage.equals(convertedMessage)
                ? originalMessage
                : convertedMessage + " §7(" + originalMessage + ")";

        event.setFormat(clanDisplay + "%1$s: %2$s");
        event.setMessage(finalMessage);
    }
}
package com.lunar_prototype.deepwither.party;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class Party {
    private final UUID leaderId;
    private final Set<UUID> members;

    public Party(UUID leaderId) {
        this.leaderId = leaderId;
        this.members = new HashSet<>();
        this.members.add(leaderId); // リーダーもメンバーに含める
    }

    public void addMember(UUID playerId) {
        members.add(playerId);
    }

    public void removeMember(UUID playerId) {
        members.remove(playerId);
    }

    public boolean isMember(UUID playerId) {
        return members.contains(playerId);
    }

    public Set<UUID> getMemberIds() {
        return members;
    }

    public UUID getLeaderId() {
        return leaderId;
    }

    // オンラインのメンバー（Playerオブジェクト）を取得するヘルパー
    public Set<Player> getOnlineMembers() {
        return members.stream()
                .map(Bukkit::getPlayer)
                .filter(p -> p != null && p.isOnline())
                .collect(Collectors.toSet());
    }
}
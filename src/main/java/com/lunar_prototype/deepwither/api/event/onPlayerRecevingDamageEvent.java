//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by Fernflower decompiler)
//

package com.lunar_prototype.deepwither.api.event;

import org.antlr.v4.runtime.misc.NotNull;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class onPlayerRecevingDamageEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    private final Player victim;
    private final LivingEntity attacker;
    private final Double damage;

    public onPlayerRecevingDamageEvent(@NotNull Player player, @NotNull LivingEntity attacker, Double damage) {
        this.victim = player;
        this.attacker = attacker;
        this.damage = damage;
    }

    @NotNull
    public Player getvictim() {
        return this.victim;
    }

    @NotNull
    public LivingEntity getattacker() {
        return this.attacker;
    }

    @NotNull
    public Double getdamage() {
        return this.damage;
    }

    @NotNull
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
}

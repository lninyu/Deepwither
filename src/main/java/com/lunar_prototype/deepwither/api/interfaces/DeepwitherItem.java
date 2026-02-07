package com.lunar_prototype.deepwither.api.interfaces;

import net.kyori.adventure.text.Component;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;

import java.util.List;

public interface DeepwitherItem extends Keyed {
    Component defaultName();
    List<Component> defaultLore();
    NamespacedKey category();
}

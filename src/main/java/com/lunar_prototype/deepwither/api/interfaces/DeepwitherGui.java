package com.lunar_prototype.deepwither.api.interfaces;

import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public interface DeepwitherGui extends Keyed {
    boolean open(@NotNull Player player);
}

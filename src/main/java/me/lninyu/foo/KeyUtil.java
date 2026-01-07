package me.lninyu.foo;

import com.lunar_prototype.deepwither.Deepwither;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;

public final class KeyUtil {
    private static final Plugin DEEPWITHER = JavaPlugin.getPlugin(Deepwither.class);
    public static final NamespacedKey UNKNOWN = NamespacedKey.fromString("unknown", DEEPWITHER);

    public static NamespacedKey create(@NotNull String string) {
        return Optional.ofNullable(NamespacedKey.fromString(string.toLowerCase(), DEEPWITHER)).orElse(UNKNOWN);
    }
}

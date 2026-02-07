package com.lunar_prototype.deepwither.common;

import io.papermc.paper.persistence.PersistentDataViewHolder;
import org.bukkit.NamespacedKey;
import org.bukkit.persistence.PersistentDataHolder;
import org.bukkit.persistence.PersistentDataType;
import org.jspecify.annotations.NullMarked;

import java.util.Optional;

@NullMarked
public record PersistentDataKey<P, C>(NamespacedKey key, PersistentDataType<P, C> type) {
    public static <P, C> Optional<C> get(PersistentDataViewHolder holder, PersistentDataKey<P, C> dataKey) {
        return Optional.ofNullable(holder.getPersistentDataContainer().get(dataKey.key, dataKey.type));
    }

    public static <P, C> void set(PersistentDataHolder holder, PersistentDataKey<P, C> dataKey, C value) {
        holder.getPersistentDataContainer().set(dataKey.key, dataKey.type, value);
    }

    public static <P, C> void remove(PersistentDataHolder holder, PersistentDataKey<P, C> dataKey) {
        var container = holder.getPersistentDataContainer();

        if (container.has(dataKey.key, dataKey.type)) {
            container.remove(dataKey.key);
        }
    }
}

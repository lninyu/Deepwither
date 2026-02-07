package com.lunar_prototype.deepwither.labs;

import com.lunar_prototype.deepwither.api.interfaces.DeepwitherGui;
import com.lunar_prototype.deepwither.api.interfaces.DeepwitherItem;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Stream;

@NullMarked
public final class DeepwitherRegistry<T extends Keyed> implements Iterable<T> {
    public static final DeepwitherRegistry<DeepwitherGui> GUI = create();
    public static final DeepwitherRegistry<DeepwitherItem> ITEMS = create();

    private Map<NamespacedKey, T> registry;
    private boolean freeze = false;

    private static <K extends Keyed> DeepwitherRegistry<K> create() {
        return new DeepwitherRegistry<>();
    }

    private DeepwitherRegistry() {
        this.registry = new HashMap<>();
    }

    public void register(T value) {
        freezeCheck();
        this.registry.put(value.getKey(), value);
    }

    @Nullable
    public T get(NamespacedKey key) {
        return this.registry.get(key);
    }

    public void freeze() {
        freezeCheck();
        this.registry = Map.copyOf(this.registry);
        this.freeze = true;
    }

    private void freezeCheck() {
        if (this.freeze) {
            throw new IllegalStateException("Cannot freeze more than once");
        }
    }

    @Override
    public Iterator<T> iterator() {
        return this.registry.values().iterator();
    }

    public Stream<T> stream() {
        return this.registry.values().stream();
    }

    public Stream<NamespacedKey> keyStream() {
        return this.registry.keySet().stream();
    }
}

package com.lunar_prototype.deepwither.api.database;

import java.util.LinkedHashMap;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public abstract class DatabaseFacade<K, V> implements AutoCloseable {
    /**
     * キャッシュの参照を返却。
     */
    protected abstract LinkedHashMap<K, V> cache();
    protected abstract int limit();

    /**
     * @return キャッシュにデータが存在するなら{@code true}。
     */
    public boolean has(K k) {
        return cache().containsKey(k);
    }

    /**
     * キャッシュにデータを配置。
     */
    public void put(K k, V v) {
        var cache = cache();
        if (cache.size() >= limit() && !cache.containsKey(k)) {
            var eldest = cache.firstEntry();
            if (eldest != null) {
                var key = eldest.getKey();
                flush(key);
                evict(key);
            }
        }

        cache.put(k, v);
    }

    /**
     * キャッシュに格納されたデータの取得。
     * @return キャッシュに格納されたデータ
     */
    public Optional<V> get(K k) {
        return Optional.ofNullable(cache().get(k));
    }

    /**
     * キャッシュに格納されたデータの返却を試み、存在しない場合は{@link #fetch(K)}を呼び出す。
     * @return 取得したデータ
     */
    public CompletableFuture<V> getOrFetch(K k) {
        return get(k).map(CompletableFuture::completedFuture).orElseGet(() -> fetch(k));
    }

    /**
     * データベースからデータを取得し、キャッシュに格納。
     * @return 取得したデータ
     */
    public abstract CompletableFuture<V> fetch(K k);

    /**
     * キャッシュに格納されたデータをデータベースに保存。
     */
    public abstract void flush(K k);

    /**
     * キャッシュに格納された全てのデータをデータベースに保存。
     */
    public abstract void flushAll();

    /**
     * キャッシュに格納されたデータを除去。
     */
    public void evict(K k) {
        cache().remove(k);
    }

    /**
     * キャッシュに格納された全てのデータを全て除去。
     */
    public void evictAll() {
        cache().clear();
    }

    /**
     * おまえ なまえ よむ りかい
     */
    public void flushThenEvict(K k) {
        flush(k);
        evict(k);
    }

    /**
     * キャッシュとデータベースから削除。
     */
    public abstract void delete(K k);

    @Override
    public void close() {
        flushAll();
        evictAll();
    }
}

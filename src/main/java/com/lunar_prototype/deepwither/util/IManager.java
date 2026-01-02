package com.lunar_prototype.deepwither.util;

public interface IManager {
    /**
     * @throws Exception 初期化に失敗した場合。
     * これにより、メインクラスでエラーをキャッチしてプラグインを安全に停止できる。
     */
    void init() throws Exception;

    /**
     * プラグイン停止時の保存/終了処理
     */
    default void shutdown() {}
}

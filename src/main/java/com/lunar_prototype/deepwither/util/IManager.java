package com.lunar_prototype.deepwither.util;

public interface IManager {
    /**
     * プラグイン起動時の初期化処理
     */
    void init();

    /**
     * プラグイン停止時の保存/終了処理
     */
    default void shutdown() {}
}

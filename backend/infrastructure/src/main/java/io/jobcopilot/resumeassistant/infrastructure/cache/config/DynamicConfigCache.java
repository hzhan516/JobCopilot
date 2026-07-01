package io.jobcopilot.resumeassistant.infrastructure.cache.config;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 动态配置本地缓存 / Local cache for dynamic configuration values.
 * <p>
 * 后端实例通过 Redis Pub/Sub 接收配置变更通知后，将最新值写入该缓存，
 * 供本地快速读取。每个 JVM 实例独立维护一份内存副本。
 * Backend instances write the latest config values here after receiving
 * change notifications via Redis Pub/Sub, for fast local reads.
 */
public final class DynamicConfigCache {

    private static final ConcurrentHashMap<String, String> CACHE = new ConcurrentHashMap<>();

    private DynamicConfigCache() {
        // utility class
    }

    /**
     * 写入或更新缓存项 / Put or update a cached entry.
     */
    public static void put(String key, String value) {
        CACHE.put(key, value);
    }

    /**
     * 读取缓存项 / Read a cached entry.
     */
    public static String get(String key) {
        return CACHE.get(key);
    }

    /**
     * 清空缓存 / Clear all cached entries.
     */
    public static void clear() {
        CACHE.clear();
    }
}

package ru.semavin.telegrambot.services.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.ArrayList;

@Service
@Slf4j
@RequiredArgsConstructor
public class CacheUtil {
    private final CacheManager cacheManager;

    /**
     * Удаляем все ключи в кэше cacheName, начинающиеся на groupName + '-'.
     */
    public void evictAllGroupKeys(String cacheName, String groupName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            log.warn("Кэш {} не найден в CacheManager", cacheName);
            return;
        }

        Object nativeCache = cache.getNativeCache();
        if (nativeCache instanceof com.github.benmanes.caffeine.cache.Cache<?, ?> caffeineSync) {
            // Синхронный вариант
            var mapView = caffeineSync.asMap();
            for (Object keyObj : new ArrayList<>(mapView.keySet())) {
                if (keyObj instanceof String keyStr && keyStr.startsWith(groupName + "-")) {
                    mapView.remove(keyStr);
                    log.debug("Удален ключ '{}' из кэша '{}'", keyStr, cacheName);
                }
            }
        } else if (nativeCache instanceof com.github.benmanes.caffeine.cache.AsyncCache<?, ?> caffeineAsync) {
            // Асинхронный вариант
            var syncCache = caffeineAsync.synchronous();
            var mapView = syncCache.asMap();
            for (Object keyObj : new ArrayList<>(mapView.keySet())) {
                if (keyObj instanceof String keyStr && keyStr.startsWith(groupName + "-")) {
                    mapView.remove(keyStr);
                    log.debug("Удален (async) ключ '{}' из кэша '{}'", keyStr, cacheName);
                }
            }
        } else if (nativeCache instanceof java.util.Map<?, ?> map) {
            // Если ConcurrentMapCache (Spring дефолт)
            for (Object keyObj : new ArrayList<>(map.keySet())) {
                if (keyObj instanceof String keyStr && keyStr.startsWith(groupName + "-")) {
                    map.remove(keyStr);
                    log.debug("Удален ключ '{}' из кэша '{}'", keyStr, cacheName);
                }
            }
        } else {
            log.warn("Кэш '{}' имеет неподдерживаемый тип: {}", cacheName, nativeCache.getClass());
        }
    }
}

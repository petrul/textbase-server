package ro.editii.scriptorium.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Component;

/**
 * extension of {@link CacheManager}
 */
@Component @RequiredArgsConstructor
public class CacheMan {

    final CacheManager cacheManager;

    public void clearAll() {
        final var names = this.cacheManager.getCacheNames();
        for (String name: names) {
            final var cache = this.cacheManager.getCache(name);
            cache.clear();
        }
    }
}

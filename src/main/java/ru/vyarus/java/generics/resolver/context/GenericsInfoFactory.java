package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericInfoUtils;

import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Analyze class hierarchy and produce object with resolved generics for each class. Resolved generics descriptors
 * are cached (for not inlying contexts).
 * <p>
 * Note: when ignore classes used, cache will not work: such descriptors are always resolved.
 * <p>
 * Cache may be disabled (e.g. when JRebel used) by using environment variable or system property e.g.:
 * {@code System.setProperty(GenericsInfoFactory.CACHE_PROPERTY, 'false')}.
 * Property value checked on cache write. To clear current cache state use static method.
 *
 * @author Vyacheslav Rusakov
 * @since 16.10.2014
 */
public final class GenericsInfoFactory {

    /**
     * System property or environment variable name to disable cache.
     * If value is 'false' - cache disabled, otherwise cache enabled.
     */
    public static final String CACHE_PROPERTY = GenericsInfoFactory.class.getName() + ".cache";

    private static final Map<Class<?>, GenericsInfo> CACHE = new WeakHashMap<Class<?>, GenericsInfo>();
    // lock will not affect performance for cached descriptors, just to make sure nothing was build two times
    private static final ReentrantLock LOCK = new ReentrantLock();

    private GenericsInfoFactory() {
    }

    /**
     * Note: ignore classes switch off caching for resolved descriptor (and if completely resolved version
     * contained in cache limited version will be composed one more time).
     *
     * @param type          finder type to investigate
     * @param ignoreClasses list of classes to ignore during inspection (useful to avoid interface clashes)
     * @return descriptor for class hierarchy generics substitution
     */
    public static GenericsInfo create(final Class<?> type, final Class<?>... ignoreClasses) {
        GenericsInfo descriptor = ignoreClasses.length > 0
                ? GenericInfoUtils.create(type, ignoreClasses) : CACHE.get(type);
        if (descriptor == null) {
            LOCK.lock();
            try {
                if (CACHE.get(type) != null) {
                    // descriptor could be created while thread wait for lock
                    descriptor = CACHE.get(type);
                } else {
                    descriptor = GenericInfoUtils.create(type);
                    if (isCacheEnabled()) {
                        // internal check
                        if (CACHE.get(type) != null) {
                            throw new ConcurrentModificationException("Descriptor already present in cache");
                        }
                        CACHE.put(type, descriptor);
                    }
                }
            } finally {
                LOCK.unlock();
            }
        }
        return descriptor;
    }

    /**
     * Clears cached descriptors (already parsed).
     * Cache could be completely disabled using system property or environment variable
     *
     * @see #CACHE_PROPERTY
     */
    public static void clearCache() {
        LOCK.lock();
        try {
            CACHE.clear();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Disables descriptors cache.
     */
    public static void disableCache() {
        System.setProperty(CACHE_PROPERTY, Boolean.FALSE.toString());
    }

    /**
     * @return true is cache enabled, false otherwise
     */
    public static boolean isCacheEnabled() {
        final String no = Boolean.FALSE.toString();
        return !no.equals(System.getenv(CACHE_PROPERTY))
                && !no.equals(System.getProperty(CACHE_PROPERTY));
    }
}

package ru.vyarus.java.generics.resolver;

import ru.vyarus.java.generics.resolver.context.GenericsContext;
import ru.vyarus.java.generics.resolver.context.GenericsInfoFactory;

/**
 * Resolves class hierarchy generics and provides api for introspection.
 *
 * @author Vyacheslav Rusakov
 * @since 17.11.2014
 */
public final class GenericsResolver {

    private GenericsResolver() {
    }

    /**
     * By default returned context set on root class (but generic types for root class will be resolved from specified
     * generics bounds). To use it switch context to required type from hierarchy:
     * {@code returnedContext.type(SomeTypeFromHierarchy.class)}.
     * <p>Note: when ignore classes provided, produced {@code GenericsInfo} instance will not be cached
     * (and full version from cache will not be used also)</p>
     *
     * @param type root class to resolve generics hierarchy
     * @param ignoreClasses list of classes to ignore during inspection (useful to avoid interface clashes
     *                      or to limit resolution depth)
     * @return resolved generics context object
     */
    public static GenericsContext resolve(final Class<?> type, final Class<?>... ignoreClasses) {
        return new GenericsContext(GenericsInfoFactory.create(type, ignoreClasses), type);
    }
}

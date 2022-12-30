package ru.vyarus.java.generics.resolver.util.map;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Special map for defining ignorable generics in order to resolve unknown generic variable to simple Object.
 * Supposed to be used with utility classes directly (when appropriate). For example,
 * {@code GenericsUtils.resolveClass(Type, new IgnoreGenericsMap(generics)}.
 * <p>
 * Does not allow modification (except initial initialization on creation).
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
public final class IgnoreGenericsMap extends LinkedHashMap<String, Type> {

    private static final IgnoreGenericsMap INSTANCE = new IgnoreGenericsMap();

    public IgnoreGenericsMap() {
        // default
    }

    public IgnoreGenericsMap(final Map<? extends String, ? extends Type> m) {
        super(m);
    }

    @Override
    public Type get(final Object key) {
        // always Object for unknown generic name
        final Type res = super.get(key);
        return res == null ? Object.class : res;
    }

    @Override
    public Type put(final String key, final Type value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void putAll(final Map<? extends String, ? extends Type> m) {
        throw new UnsupportedOperationException();
    }

    /**
     * @return shared map instance
     */
    @SuppressFBWarnings("MS_EXPOSE_REP")
    public static IgnoreGenericsMap getInstance() {
        return INSTANCE;
    }
}

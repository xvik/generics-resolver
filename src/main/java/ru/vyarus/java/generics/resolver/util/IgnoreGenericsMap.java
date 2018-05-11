package ru.vyarus.java.generics.resolver.util;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Special map for defining ignorable generics in order to resolve unknown generic variable to simple Object.
 * Supposed to be used with utility classes directly (when appropriate). For example,
 * {@code GenericsUtils.resolveClass(Type, new IgnoreGenericsMap(generics)}.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
public final class IgnoreGenericsMap extends HashMap<String, Type> {

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
}

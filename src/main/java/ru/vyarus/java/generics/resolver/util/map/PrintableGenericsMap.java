package ru.vyarus.java.generics.resolver.util.map;

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Special map to use with {@link ru.vyarus.java.generics.resolver.util.TypeToStringUtils} in order to see generic
 * name in the resulted string. For example, {@code List<T> instead of List<Object>}.
 *
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
public class PrintableGenericsMap extends LinkedHashMap<String, Type> {

    public PrintableGenericsMap() {
        // default
    }

    public PrintableGenericsMap(final Map<? extends String, ? extends Type> m) {
        super(m);
    }

    @Override
    public Type get(final Object key) {
        // always Object for unknown generic name
        final Type res = super.get(key);
        return res == null ? new ExplicitTypeVariable((String) key) : res;
    }
}

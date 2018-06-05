package ru.vyarus.java.generics.resolver.util.map;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Special map used as default empty generics map. Prevents elements addition.
 *
 * @author Vyacheslav Rusakov
 * @since 05.06.2018
 */
public class EmptyGenericsMap extends LinkedHashMap<String, Type> {

    private static final EmptyGenericsMap INSTANCE = new EmptyGenericsMap();

    public EmptyGenericsMap() {
        super(0);
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
    public static EmptyGenericsMap getInstance() {
        return INSTANCE;
    }
}

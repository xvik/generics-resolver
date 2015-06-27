package ru.vyarus.java.generics.resolver.context;

import java.lang.reflect.Type;
import java.util.Map;

/**
 * Generics context of specific type (class).
 *
 * @author Vyacheslav Rusakov
 * @see GenericsContext
 * @since 26.06.2015
 */
public class TypeGenericsContext extends GenericsContext {

    public TypeGenericsContext(final GenericsInfo genericsInfo, final Class<?> type) {
        super(genericsInfo, type);
    }

    @Override
    protected Map<String, Type> contextGenerics() {
        return typeGenerics;
    }
}

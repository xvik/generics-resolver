package ru.vyarus.java.generics.resolver.context.container;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collections;

/**
 * Wrapper to hold resolved array type.
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2014
 */
public class GenericArrayTypeImpl implements GenericArrayType {

    private final Type componentType;

    public GenericArrayTypeImpl(final Type componentType) {
        this.componentType = componentType;
    }

    @Override
    public Type getGenericComponentType() {
        return componentType;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this, Collections.<String, Type>emptyMap());
    }
}

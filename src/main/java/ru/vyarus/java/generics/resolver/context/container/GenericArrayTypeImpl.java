package ru.vyarus.java.generics.resolver.context.container;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

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
        if (componentType == null) {
            throw new IllegalArgumentException("Null component type is not allowed");
        }
    }

    @Override
    public Type getGenericComponentType() {
        return componentType;
    }

    @Override
    public boolean equals(final Object o) {
        boolean res = this == o;
        if (!res && o instanceof GenericArrayType) {
            final Type thatComponentType = ((GenericArrayType) o).getGenericComponentType();
            res = componentType.equals(thatComponentType);
        }
        return res;
    }

    @Override
    public int hashCode() {
        return componentType.hashCode();
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this);
    }
}

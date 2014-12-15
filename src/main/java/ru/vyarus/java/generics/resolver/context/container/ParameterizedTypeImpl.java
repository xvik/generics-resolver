package ru.vyarus.java.generics.resolver.context.container;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;

/**
 * Wrapper to hold resolved parametrization.
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2014
 */
public class ParameterizedTypeImpl implements ParameterizedType {

    private final Type rawType;
    private final Type[] actualArguments;
    private final Type ownerType;

    public ParameterizedTypeImpl(final Type rawType, final Type[] actualArguments, final Type ownerType) {
        this.rawType = rawType;
        this.actualArguments = Arrays.copyOf(actualArguments, actualArguments.length);
        this.ownerType = ownerType;
    }

    @Override
    public Type[] getActualTypeArguments() {
        return Arrays.copyOf(actualArguments, actualArguments.length);
    }

    @Override
    public Type getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this, Collections.<String, Type>emptyMap());
    }
}

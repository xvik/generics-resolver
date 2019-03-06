package ru.vyarus.java.generics.resolver.context.container;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;

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

    public ParameterizedTypeImpl(final Type rawType, final Type... actualArguments) {
        this(rawType, actualArguments, null);
    }

    public ParameterizedTypeImpl(final Type rawType, final Type[] actualArguments, final Type ownerType) {
        this.rawType = rawType;
        this.actualArguments = Arrays.copyOf(actualArguments, actualArguments.length);
        this.ownerType = ownerType;
        if (rawType == null) {
            throw new IllegalArgumentException("Null raw type is not allowed");
        }
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
    public boolean equals(final Object o) {
        boolean res = this == o;
        if (!res && o instanceof ParameterizedType) {
            final ParameterizedType that = (ParameterizedType) o;
            final Type[] thatActualArguments = that.getActualTypeArguments();
            final Type thatOwnerType = that.getOwnerType();
            final Type thatRawType = that.getRawType();

            res = rawType.equals(thatRawType)
                    && Arrays.equals(actualArguments, thatActualArguments)
                    && (ownerType != null ? ownerType.equals(thatOwnerType) : thatOwnerType == null);
        }
        return res;
    }

    @Override
    public int hashCode() {
        int result = rawType.hashCode();
        result = 31 * result + Arrays.hashCode(actualArguments);
        result = 31 * result + (ownerType != null ? ownerType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this);
    }
}

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
    @SuppressWarnings({"checkstyle:needbraces", "PMD.IfStmtsMustUseBraces", "PMD.OnlyOneReturn"})
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof ParameterizedTypeImpl)) return false;

        final ParameterizedTypeImpl that = (ParameterizedTypeImpl) o;

        if (!Arrays.equals(actualArguments, that.actualArguments)) return false;
        if (ownerType != null ? !ownerType.equals(that.ownerType) : that.ownerType != null) return false;
        if (rawType != null ? !rawType.equals(that.rawType) : that.rawType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = rawType != null ? rawType.hashCode() : 0;
        result = 31 * result + (actualArguments != null ? Arrays.hashCode(actualArguments) : 0);
        result = 31 * result + (ownerType != null ? ownerType.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this, Collections.<String, Type>emptyMap());
    }
}

package ru.vyarus.java.generics.resolver.context.container;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;

/**
 * Wrapper to hold resolved bounds types.
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2014
 */
public class WildcardTypeImpl implements WildcardType {

    private final Type[] upperBounds;
    private final Type[] lowerBounds;

    @SuppressWarnings("PMD.UseVarargs")
    public WildcardTypeImpl(final Type[] upperBounds, final Type[] lowerBounds) {
        this.upperBounds = Arrays.copyOf(upperBounds, upperBounds.length);
        this.lowerBounds = Arrays.copyOf(lowerBounds, lowerBounds.length);
    }

    @Override
    public Type[] getLowerBounds() {
        return Arrays.copyOf(lowerBounds, lowerBounds.length);
    }

    @Override
    public Type[] getUpperBounds() {
        return Arrays.copyOf(upperBounds, upperBounds.length);
    }

    @Override
    @SuppressWarnings({"checkstyle:needbraces", "PMD.IfStmtsMustUseBraces", "PMD.OnlyOneReturn"})
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof WildcardTypeImpl)) return false;

        final WildcardTypeImpl that = (WildcardTypeImpl) o;

        if (!Arrays.equals(lowerBounds, that.lowerBounds)) return false;
        if (!Arrays.equals(upperBounds, that.upperBounds)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = upperBounds != null ? Arrays.hashCode(upperBounds) : 0;
        result = 31 * result + (lowerBounds != null ? Arrays.hashCode(lowerBounds) : 0);
        return result;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this, Collections.<String, Type>emptyMap());
    }
}

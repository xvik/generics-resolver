package ru.vyarus.java.generics.resolver.context.container;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Collections;

/**
 * Wrapper to hold resolved bounds types.
 * <p>
 * Note: api supports multiple bounds but actually only one bound could be set (not one upper, one lower, but
 * only one upper or only one lower!).
 * <p>
 * Object is also used to hold multiple bounds of {@link java.lang.reflect.TypeVariable}.
 * For example, {@code MyClass<T extends Number & Comparable>} will be repackaged as wildcard with multiple
 * bonds {@code <? extends Number & Comparable>} (note that this is impossible declaration in real java code).
 * This should not cause any harm as, I'm sure, all reflection logic always takes first parameter and will not break.
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2014
 */
public class WildcardTypeImpl implements WildcardType {

    // ? extends T
    private final Type[] upperBounds;
    // ? super T
    private final Type[] lowerBounds;

    @SuppressWarnings("PMD.UseVarargs")
    public WildcardTypeImpl(final Type[] upperBounds, final Type[] lowerBounds) {
        this.upperBounds = Arrays.copyOf(upperBounds, upperBounds.length);
        this.lowerBounds = Arrays.copyOf(lowerBounds, lowerBounds.length);
    }

    /**
     * ? super Something. If provided then upper bound is [Object] (implicit).
     *
     * @return 0 or 1 bound.
     */
    @Override
    public Type[] getLowerBounds() {
        return Arrays.copyOf(lowerBounds, lowerBounds.length);
    }

    /**
     * ? extends Something. If provided then lower bound is empty.
     * It couldn't have &gt;=2 bounds in real life, but wildcard type is used to store named generic declaration:
     * {@code Base<T extends One & Two>} will be saved as impossible wildcard {@code <? extends One & Two>}
     *
     * @return 0, 1 or more bounds.
     */
    @Override
    public Type[] getUpperBounds() {
        return Arrays.copyOf(upperBounds, upperBounds.length);
    }

    @Override
    public boolean equals(final Object o) {
        boolean res = this == o;
        if (!res && o instanceof WildcardType) {
            final WildcardType that = (WildcardType) o;
            final Type[] thatLowerBounds = that.getLowerBounds();
            final Type[] thatUpperBounds = that.getUpperBounds();

            res = Arrays.equals(lowerBounds, thatLowerBounds) && Arrays.equals(upperBounds, thatUpperBounds);
        }
        return res;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(upperBounds);
        result = 31 * result + Arrays.hashCode(lowerBounds);
        return result;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this, Collections.<String, Type>emptyMap());
    }

    /**
     * ? extends A &amp; B.
     *
     * @param upper upper bounds
     * @return upper bounded wildcard for multiple types
     */
    public static WildcardTypeImpl upper(final Type... upper) {
        return new WildcardTypeImpl(upper, new Type[0]);
    }

    /**
     * ? super A.
     *
     * @param lower lower bound
     * @return lower bounded wildcard
     */
    public static WildcardTypeImpl lower(final Type lower) {
        // upper bound must be always present
        return new WildcardTypeImpl(new Type[]{Object.class}, new Type[]{lower});
    }
}

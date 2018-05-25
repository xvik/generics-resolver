package ru.vyarus.java.generics.resolver.util.walk;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;

/**
 * Check if one type is more specific. Stops when first type is not more specific.
 * Implicitly tracks compatibility: check {@link #isCompatible()} before
 * calling {@link #isMoreSpecific()} as the later always assume compatible hierarchies.
 * For example, {@code ArrayList<String>} is more specific then {@code List<Integer>}, but not compatible.
 * <p>
 * Note that specificity is not the same as assignability. For example {@code List} is assignable to
 * {@code List<? extends String>}, but later is more specific (actually). Other example is unknown types:
 * {@code T<String, Object>} is assignable to {@code T<String, String>}, but later is more specific
 * (contains more type information).
 *
 * @author Vyacheslav Rusakov
 * @see TypesWalker
 * @since 11.05.2018
 */
public class ComparatorTypesVisitor implements TypesVisitor {

    private final IgnoreGenericsMap ignore = new IgnoreGenericsMap();

    private boolean compatible = true;
    private boolean moreSpecific = true;

    @Override
    public boolean next(final Type one, final Type two) {
        // when right part is object consider left as more specific: in edge case, Object is more specific then Object
        if (two != Object.class) {
            // check upper bounds for wildcards (? extends A)
            final Class[] oneBounds = GenericsUtils.resolveUpperBounds(one, ignore);
            final Class[] twoBounds = GenericsUtils.resolveUpperBounds(two, ignore);

            final boolean boundsEqual = Arrays.equals(oneBounds, twoBounds);

            if (boundsEqual) {
                // for equal upper bounds wildcard's lower bound could be important e.g Object and ? super String
                // (last is more specific, while it's upper bound is Object too)
                moreSpecific = compareRightLowerBound(one, two);
            } else {
                // direct comparison for non equal objects
                // edge case: Object on the left could not be more specific
                moreSpecific = oneBounds[0] != Object.class && TypeUtils.isAssignableBounds(oneBounds, twoBounds);
            }

            // case when upper bounds are not set (Object - Object)
            moreSpecific = moreSpecific && compareLowerBounds(one, two);

            // no need to go further if context types are not equal (specificity is already obvious)
            return moreSpecific && boundsEqual;
        }
        // actually nowhere to go as Object on the right (last step)
        return true;
    }

    @Override
    public void incompatibleHierarchy(final Type one, final Type two) {
        compatible = false;
    }

    /**
     * @return true if first type is more specific, false otherwise
     */
    public boolean isMoreSpecific() {
        return moreSpecific;
    }

    /**
     * @return true when types are incompatible, false when compatible
     */
    public boolean isCompatible() {
        return compatible;
    }

    private boolean compareRightLowerBound(final Type one, final Type two) {
        final boolean oneWildcard = one instanceof WildcardType;
        final boolean twoWildcard = two instanceof WildcardType;

        if (!oneWildcard && twoWildcard
                && ((WildcardType) two).getLowerBounds().length > 0) {
            final Class<?> lowerBound = GenericsUtils
                    .resolveClass(((WildcardType) two).getLowerBounds()[0], ignore);
            // if right wildcard declares something meaningful - it's more specific
            return lowerBound == Object.class;
        }
        return true;
    }

    private boolean compareLowerBounds(final Type one, final Type two) {
        final boolean oneWildcard = one instanceof WildcardType;
        final boolean twoWildcard = two instanceof WildcardType;

        if (oneWildcard && twoWildcard) {
            final Type[] oneLow = ((WildcardType) one).getLowerBounds();
            final Type[] twoLow = ((WildcardType) two).getLowerBounds();
            // if both wildcards with lower bounds then more specific type contain lower type
            // e.g. ? super Number is more specific then ? super Integer
            if (oneLow.length > 0 && twoLow.length > 0 && twoLow[0] != Object.class) {
                // for example, ? super Integer and ? super BigInteger are not assignable
                // (in spite of the fact that they share some common types)
                // but ? super Number and ? super Integer are assignable
                // (types specificity check is inverted - not a mistake below)
                return TypeUtils.isMoreSpecific(twoLow[0], oneLow[0]);
            }
        }
        return true;
    }
}

package ru.vyarus.java.generics.resolver.util.walk;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * Compare if one type could be assigned to another (casted). Object assumed to mean unknown type: so object is
 * assignable to anything (no information on type). Object is always assignable to Object.
 * Note that {@code T<String, Object>} is assignable to {@code T<String, String>} as Object considered as unknown
 * type and so could be compatible (in opposite way is also assignable as anything is assignable to Object).
 * <p>
 * Java wildcard rules are generally not honored because at runtime they are meaningless.
 * {@code List == List<Object> == List<? super Object> == List<? extends Object>}. All upper bounds are used for
 * comparison (multiple upper bounds in wildcard could be from repackaging of generic declaration
 * {@code T<extends A&B>}. Lower bounds are taken into account as: if both have lower bound then
 * right's type bound must be higher ({@code ? extends Number and ? extends Integer}). If only left
 * type is lower bounded wildcard then it is not assignable (except Object).
 *
 * @author Vyacheslav Rusakov
 * @since 18.05.2018
 */
public class AssignabilityTypesVisitor implements TypesVisitor {
    private static final IgnoreGenericsMap IGNORE = IgnoreGenericsMap.getInstance();

    private boolean assignable = true;

    @Override
    public boolean next(final Type one, final Type two) {
        // everything could be assigned to object, so check only right is not object case
        // note that right could still be object like ? super String (which upper bound is object)
        if (two != Object.class) {
            // check upper bounds for wildcards (? extends)
            final Class[] oneBounds = GenericsUtils.resolveUpperBounds(one, IGNORE);
            final Class[] twoBounds = GenericsUtils.resolveUpperBounds(two, IGNORE);
            assignable = TypeUtils.isAssignableBounds(oneBounds, twoBounds);

            final boolean oneWildcard = one instanceof WildcardType;
            final boolean twoWildcard = two instanceof WildcardType;

            final Type[] oneLow = oneWildcard ? ((WildcardType) one).getLowerBounds() : new Type[0];
            final Type[] twoLow = twoWildcard ? ((WildcardType) two).getLowerBounds() : new Type[0];

            // check lower wildcard bounds assignability (bounds must be assignable)
            // ? extends Object ignored
            if (assignable && oneLow.length > 0 && twoLow.length > 0) {

                // for example, ? super Integer is not assignable to ? super BigInteger
                // (in spite of the fact that they share some types in common)
                // but ? super Number is assignable to ? super Integer
                // (types assignability check  for lower bound is inverted - not a mistake below)
                assignable = TypeUtils.isAssignable(twoLow[0], oneLow[0]);
            }

            if (assignable && oneLow.length > 0 && twoBounds[0] != Object.class) {
                // lower bound could not be assigned to anything else (only opposite way is possible)
                // for example, List<? super String> is not assignable to List<String>, but
                // List<String> is assignable to List<? super String>
                // (note that compatibility in last case is checked by TypesWalker itself and so
                // we can be sure its compatible here)
                assignable = false;
            }
        }
        // stop when not assignable types detected
        return assignable;
    }

    @Override
    public void incompatibleHierarchy(final Type one, final Type two) {
        assignable = false;
    }

    public boolean isAssignable() {
        return assignable;
    }
}

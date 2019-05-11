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

            // check lower wildcard bounds assignability (bounds must be assignable)
            assignable = assignable && checkLowerBounds(one, two);
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

    /**
     * Check lower bounded wildcard cases. Method is not called if upper bounds are not assignable.
     * <p>
     * When left is not lower bound - compatibility will be checked by type walker and when compatible always
     * assignable. For example, String compatible (and assignable) with ? super String and Integer is not compatible
     * with ? super Number (and not assignable).
     * <p>
     * Wen right is not lower bound, when left is then it will be never assignable. For example,
     * ? super String is not assignable to String.
     * <p>
     * When both lower wildcards: lower bounds must be from one hierarchy and left type should be lower.
     * For example,  ? super Integer and ? super BigInteger are not assignable in spite of the fact that they
     * share some common types. ? super Number is more specific then ? super Integer (super inverse meaning).
     *
     * @param one first type
     * @param two second type
     * @return true when left is assignable to right, false otherwise
     */
    private boolean checkLowerBounds(final Type one, final Type two) {
        final boolean res;
        // ? super Object is impossible here due to types cleanup in tree walker
        if (notLowerBounded(one)) {
            // walker will check compatibility, and compatible type is always assignable to lower bounded wildcard
            // e.g. Number assignable to ? super Number, but Integer not assignable to ? super Number
            res = true;
        } else if (notLowerBounded(two)) {
            // lower bound could not be assigned to anything else (only opposite way is possible)
            // for example, List<? super String> is not assignable to List<String>, but
            // List<String> is assignable to List<? super String> (previous condition)
            res = false;
        } else {
            // left type's bound must be lower: not a mistake! left (super inversion)!
            res = TypeUtils.isAssignable(
                    ((WildcardType) two).getLowerBounds()[0],
                    ((WildcardType) one).getLowerBounds()[0]);
        }
        return res;
    }

    private boolean notLowerBounded(final Type type) {
        return !(type instanceof WildcardType) || ((WildcardType) type).getLowerBounds().length == 0;
    }
}

package ru.vyarus.java.generics.resolver.util.walk;

import ru.vyarus.java.generics.resolver.util.ArrayTypeUtils;
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Deep types analysis utility.
 * <p>
 * Algorithm:
 * <ul>
 * <li>If types are not compatible - notify fail</li>
 * <li>Ask visitor to continue</li>
 * <li>If one if types is Object - stop</li>
 * <li>Look if types are arrays and cycle with actual array type</li>
 * <li>If types contains generics ({@code T&lt;A,B>}) then align types (resolve hierarchy for upper type and
 * compare generics of lower types) and cycle for each generic pair</li>
 * </ul>
 * <p>
 * Generics rules are not honored. For example, {@code List<?>} is considered as {@code List<Object>} and
 * assumed compatible with any list. {@code <? extends Something>} is considered as {@code Something} and
 * compatible with any subtype.
 * <p>
 * The only exception is wildcard's lower bound: {@code <? super Something>} is compatible with any super type
 * of {@code Something} but not compatible with any subtype.
 * <p>
 * If generic declares multiple bounds {@code T extends A & B} and it was not resolved with actual type
 * (sub type did not declare exact generic for it), then both bounds will be used for comparison.
 * <p>
 * When one of types is primitive then wrapper class used instead (e.g. Integer instead of int) to simplify
 * visitor logic.
 * <p>
 * NOTE: all variables in type ({@link java.lang.reflect.TypeVariable}) will be replaced in types into
 * lower bound. If you need to preserve variables (e.g. to match variable), make them explicit with
 * {@link ru.vyarus.java.generics.resolver.util.TypeVariableUtils#preserveVariables(Type)}.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
public final class TypesWalker {
    private static final IgnoreGenericsMap IGNORE_VARS = IgnoreGenericsMap.getInstance();
    @SuppressWarnings("unchecked")
    private static final List<Class<?>> STOP_TYPES = Arrays.asList(Object.class, Enum.class);

    private TypesWalker() {
    }

    /**
     * Walk will stop if visitor tells it's enough or when hierarchy incompatibility will be found.
     *
     * @param one     first type
     * @param two     second type
     * @param visitor visitor
     */
    public static void walk(final Type one, final Type two, final TypesVisitor visitor) {
        // Use possibly more specific generics (otherwise root class generics would be used as Object and this
        // way it could be used as upper bound)
        // Also, types could contain outer class generics declarations, which must be preserved
        // e.g. (Outer<String>.Inner field).getGenericType() == ParameterizedType with parametrized owner
        // Wrapped into ignore map for very specific case, when type is TypeVariable
        final Map<String, Type> oneKnownGenerics =
                new IgnoreGenericsMap(GenericsResolutionUtils.resolveGenerics(one, IGNORE_VARS));
        final Map<String, Type> twoKnownGenerics =
                new IgnoreGenericsMap(GenericsResolutionUtils.resolveGenerics(two, IGNORE_VARS));
        // Resolve variables mainly to simplify empty wildcards (? extends Object and ? super Object) to Object
        // Still have to pass map because of possibly declared outer class generics. Note that for all
        // types operations ignoring map could be used as we already replaced all variables. These generics
        // are required only for type context building (on some level to resolve comparable type)
        doWalkOuterClass(GenericsUtils.resolveTypeVariables(one, oneKnownGenerics), oneKnownGenerics,
                GenericsUtils.resolveTypeVariables(two, twoKnownGenerics), twoKnownGenerics, visitor);
    }

    /**
     * It is important to check outer classes, because their generics affect types compatibility.
     * E.g. {@code Outer<String>.Inner<C, D>} is not equal to {@code Outer<Integer>.Inner<C, D>}.
     *
     * @param one              first type
     * @param oneKnownGenerics first type generics
     * @param two              second type
     * @param twoKnownGenerics second type generics
     * @param visitor          visitor
     */
    private static void doWalkOuterClass(final Type one, final Map<String, Type> oneKnownGenerics,
                                         final Type two, final Map<String, Type> twoKnownGenerics,
                                         final TypesVisitor visitor) {
        // note: if one or two is self-constructed parameterized type, it may not contain outer (not correct container)
        final Type outerOne = TypeUtils.getOuter(one);
        final Type outerTwo = TypeUtils.getOuter(two);
        if ((outerOne == null || outerTwo == null) && (outerOne != null || outerTwo != null)) {
            // it does not make sense to go further as one type is inner and another is not
            visitor.incompatibleHierarchy(outerOne, outerTwo);
        }

        boolean walk = true;
        if (outerOne != null) {
            // walk through outer class hierarchy first
            // note that only "visible" generics from inner class are counted
            walk = doWalk(outerOne,
                    new IgnoreGenericsMap(
                            GenericsUtils.extractOwnerGenerics(GenericsUtils.resolveClass(one), oneKnownGenerics)),
                    outerTwo,
                    new IgnoreGenericsMap(
                            GenericsUtils.extractOwnerGenerics(GenericsUtils.resolveClass(two), twoKnownGenerics)),
                    visitor);
        }

        // continue walking on inner type
        if (walk) {
            doWalk(one, oneKnownGenerics, two, twoKnownGenerics, visitor);
        }
    }

    private static boolean doWalk(final Type one, final Map<String, Type> oneKnownGenerics,
                                  final Type two, final Map<String, Type> twoKnownGenerics,
                                  final TypesVisitor visitor) {
        boolean canContinue = true;
        // avoid primitives to simplify comparisons
        final Class<?> oneType = TypeUtils.wrapPrimitive(GenericsUtils.resolveClassIgnoringVariables(one));
        final Class<?> twoType = TypeUtils.wrapPrimitive(GenericsUtils.resolveClassIgnoringVariables(two));

        // direct compatibility
        if (!isCompatible(one, two)) {
            // this point must stop future processing
            visitor.incompatibleHierarchy(one, two);
            canContinue = false;
        } else if (visitor.next(one, two) && !STOP_TYPES.contains(oneType) && !STOP_TYPES.contains(twoType)) {
            // user stop or nowhere to go from object

            // classes are already checked to be compatible (isCompatible) so either both arrays or both not
            if (oneType.isArray()) {
                canContinue = doWalk(ArrayTypeUtils.getArrayComponentType(one), oneKnownGenerics,
                        ArrayTypeUtils.getArrayComponentType(two), twoKnownGenerics, visitor);
            } else if (oneType.getTypeParameters().length > 0 || twoType.getTypeParameters().length > 0) {
                // check generics compatibility
                canContinue = visitGenerics(one, oneType, oneKnownGenerics, two, twoType, twoKnownGenerics, visitor);
            }
        }
        return canContinue;
    }

    @SuppressWarnings({"checkstyle:NPathComplexity", "checkstyle:CyclomaticComplexity",
            "PMD.NPathComplexity", "PMD.CyclomaticComplexity"})
    private static boolean visitGenerics(final Type one, final Class<?> oneType,
                                         final Map<String, Type> oneKnownGenerics,
                                         final Type two, final Class<?> twoType,
                                         final Map<String, Type> twoKnownGenerics,
                                         final TypesVisitor visitor) {

        // unify types first to compare generics of the same types
        // for example List<T> and ArrayList<T>, lower type is List<T>
        // lower generics could be resolved directly, but to get generics on upper type type hierarchy must be resolved
        final boolean oneLower = oneType.isAssignableFrom(twoType);

        final Class<?> lowerClass = oneLower ? oneType : twoType;
        final Class<?> upperClass = oneLower ? twoType : oneType;

        final Type lowerType = oneLower ? one : two;
        final Type upperType = oneLower ? two : one;

        final Map<String, Type> lowerKnownGenerics = oneLower ? oneKnownGenerics : twoKnownGenerics;
        final Map<String, Type> upperKnownGenerics = oneLower ? twoKnownGenerics : oneKnownGenerics;

        final Map<String, Type> lowerGenerics = GenericsResolutionUtils.resolveGenerics(lowerType, lowerKnownGenerics);
        final Map<String, Type> upperGenerics = resolveUpperGenerics(lowerType, lowerClass, lowerKnownGenerics,
                upperType, upperClass, upperKnownGenerics);

        // generics must be compared with correct sides (otherwise real comparision is impossible)
        final Map<String, Type> oneGenerics = oneLower ? lowerGenerics : upperGenerics;
        final Map<String, Type> twoGenerics = oneLower ? upperGenerics : lowerGenerics;
        // checking only class generics, avoiding possible outer class generics (present if class is inner)
        // because even if outer generics are different they could not participate in comparing types,
        // and if outer generics participate - type's generics will be already affected
        for (Map.Entry<String, Type> entry : GenericsUtils
                .extractTypeGenerics(oneLower ? oneType : twoType, oneGenerics).entrySet()) {
            final String generic = entry.getKey();
            final Type oneParam = entry.getValue();
            final Type twoParam = twoGenerics.get(generic);
            // direct cycle case Something<T extends Something<T>> (without explicit detection will go to cycle)
            if (isGenericLoop(lowerClass, generic, oneParam)) {
                // here we compared resolved generic value with lower of original classes (because lower class will
                // be the source for both generics)
                continue;
            }
            if (!doWalk(oneParam, oneKnownGenerics, twoParam, twoKnownGenerics, visitor)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Declarations like {@code Some<T extends Some<T>>} could cause infinite analysis cycles without proper detection.
     *
     * @param src         generic declaration class
     * @param genericName generic name
     * @param genericType actual generic value
     * @return true if cycle detected, false otherwise
     */
    private static boolean isGenericLoop(final Class<?> src, final String genericName, final Type genericType) {
        // to avoid redundant checks, first look if same type is declared in parameter
        if (src.isAssignableFrom(GenericsUtils.resolveClass(genericType))) {
            // look if this generic declaration reference itself (Some<T extends Some<T>>)
            for (TypeVariable var : src.getTypeParameters()) {
                if (var.getName().equals(genericName)) {
                    for (Type bound : var.getBounds()) {
                        // declaration through the same type found  (Some<T extends Some>)
                        if (bound instanceof ParameterizedType
                                && ((ParameterizedType) bound).getRawType().equals(src)) {
                            for (Type param : ((ParameterizedType) bound).getActualTypeArguments()) {
                                // loop detected (recursive generic declaration)
                                if (param instanceof TypeVariable
                                        && ((TypeVariable) param).getName().equals(genericName)) {
                                    return false;
                                }
                            }
                            break;
                        }
                    }
                    break;
                }
            }
        }
        return false;
    }

    /**
     * Check if one types are wildcards and apply wildcard rules:
     * <ul>
     * <li>Wildcard with multiple upper bounds (repackaged T extends A & B) must be checked against all bounds</li>
     * <li>Wildcard with lower bound (? super A) compatible only with A and it's supertypes</li>
     * </ul>
     * <p>
     * Non wildcard types are projected to class
     *
     * @param one type to check
     * @param two type to check with
     * @return true if types compatible according to wildcard rules
     */
    @SuppressWarnings("checkstyle:CyclomaticComplexity")
    private static boolean isCompatible(final Type one, final Type two) {
        final Class[] oneBounds = GenericsUtils.resolveUpperBounds(one, IGNORE_VARS);
        final Class[] twoBounds = GenericsUtils.resolveUpperBounds(two, IGNORE_VARS);

        boolean res = true;
        final boolean oneWildcard = one instanceof WildcardType;
        final boolean twoWildcard = two instanceof WildcardType;
        if (oneWildcard || twoWildcard) {

            // check if both wildcards are lower bounded (? super)
            if (oneWildcard && twoWildcard) {
                res = isLowerBoundsCompatible((WildcardType) one, (WildcardType) two);
            }

            // check if left wildcard is lower bounded (? super Something)
            if (res && oneWildcard) {
                res = isLowerBoundCompatible((WildcardType) one, twoBounds);
            }

            // check if right wildcard is lower bounded (? super Something)
            if (res && twoWildcard) {
                res = isLowerBoundCompatible((WildcardType) two, oneBounds);
            }

            // compare upper bounds for compatibility (? extends Something)
            if (res) {
                res = TypeUtils.isAssignableBounds(oneBounds, twoBounds)
                        || TypeUtils.isAssignableBounds(twoBounds, oneBounds);
            }

        } else {
            // no wildcards - types must be simply assignable
            res = isCompatibleClasses(oneBounds[0], twoBounds[0]);
        }
        return res;
    }


    /**
     * Classes are compatible if one can be casted to another (or they are equal).
     *
     * @param one first class
     * @param two second class
     * @return true is classes are compatible, false otherwise
     */
    private static boolean isCompatibleClasses(final Class<?> one, final Class<?> two) {
        return one.isAssignableFrom(two) || two.isAssignableFrom(one);
    }

    /**
     * When both wildcards are lower bounded (? super) then bounds must be compatible.
     * For example, ? super Integer and ? super BigInteger are not compatible (Integer, BigInteger)
     * and ? super Comparable and ? super Number are compatible (Number assignable to Comparable).
     * <p>
     * Of course, even incompatible lower bounds share some commons (at least object) but these types
     * could not be casted to one another and so no compatibility.
     *
     * @param one first wildcard type
     * @param two second wildcard type
     * @return true if onw of wildcards is not lower bounded or lower bounds are compatible, false when
     * lower bounds are incompatible
     */
    private static boolean isLowerBoundsCompatible(final WildcardType one, final WildcardType two) {
        boolean res = true;
        final Type[] oneLower = one.getLowerBounds();
        final Type[] twoLower = two.getLowerBounds();
        if (oneLower.length > 0 && twoLower.length > 0) {
            res = isCompatible(GenericsUtils.resolveClassIgnoringVariables(oneLower[0]),
                    GenericsUtils.resolveClassIgnoringVariables(twoLower[0]));
        }
        return res;
    }

    /**
     * Check that wildcard's lower bound type is not less then any of provided types.
     *
     * @param type wildcard with possible lower bound
     * @param with types to compare with lower bound
     * @return true if compatible or no lower bound set, false otherwise
     */
    private static boolean isLowerBoundCompatible(final WildcardType type, final Class... with) {
        boolean res = true;
        if (type.getLowerBounds().length > 0) {
            // only one super could be used
            // couldn't be an object here as ? super Object is always replaced to simply Object before comparison
            final Class<?> lower = GenericsUtils.resolveClassIgnoringVariables(type.getLowerBounds()[0]);

            // target may only be lower bound's super type (or same type)
            for (Class<?> target : with) {
                if (!target.isAssignableFrom(lower)) {
                    res = false;
                    break;
                }
            }
        }
        return res;
    }

    private static Map<String, Type> resolveUpperGenerics(final Type lowerType,
                                                          final Class<?> lowerClass,
                                                          final Map<String, Type> lowerKnownGenerics,
                                                          final Type upperType,
                                                          final Class<?> upperClass,
                                                          final Map<String, Type> upperKnownGenerics) {
        final Map<String, Type> res;
        if (lowerType.equals(upperType)) {
            res = GenericsResolutionUtils
                    .resolveGenerics(upperType, upperKnownGenerics);
        } else {
            // resolve upper class hierarchy to get lower type generics
            res = GenericsResolutionUtils.resolve(upperClass,
                    // use lower generics for upper type resolution, because of possibly known owner type generics
                    GenericsResolutionUtils.resolveGenerics(upperType, lowerKnownGenerics))
                    .get(lowerClass);
        }
        return res;
    }
}

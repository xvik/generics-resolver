package ru.vyarus.java.generics.resolver.util.walk;

import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.IgnoreGenericsMap;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Deep types analysis utility.
 * <p>
 * Algorithm:
 * <ul>
 * <li>If types are not compatible - notify fail</li>
 * <li>Resolve root classes - ask visitor to continue</li>
 * <li>When one of them is object - stop processing</li>
 * <li>Look types are arrays and cycle with actual array type</li>
 * <li>If types contains generics then align types (resolve hierarchy for upper type and compare generics
 * of lower types) and cycle for each generic pair</li>
 * </ul>
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
public final class TypesWalker {

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
        final IgnoreGenericsMap ignoreVars = new IgnoreGenericsMap();
        final Class<?> oneType = GenericsUtils.resolveClass(one, ignoreVars);
        final Class<?> twoType = GenericsUtils.resolveClass(two, ignoreVars);

        // direct compatibility
        if (!isCompatible(oneType, twoType)) {
            visitor.incompatibleHierarchy(oneType, twoType);
        } else if (visitor.next(oneType, twoType) && oneType != Object.class && twoType != Object.class) {
            // user stop or nowhere to go from object

            // work with arrays
            if (oneType.isArray() || twoType.isArray()) {
                // no need to check that both arrays because classes already checked
                walk(arrayType(one), arrayType(two), visitor);
            } else if (oneType.getTypeParameters().length > 0 || twoType.getTypeParameters().length > 0) {
                // check generics compatibility
                visitGenerics(one, oneType, two, twoType, visitor);
            }
        }
    }

    private static void visitGenerics(final Type one, final Class<?> oneType,
                                      final Type two, final Class<?> twoType,
                                      final TypesVisitor visitor) {
        final IgnoreGenericsMap ignoreVars = new IgnoreGenericsMap();

        // unify types first to compare generics of the same types
        final boolean oneLower = oneType.isAssignableFrom(twoType);

        final Class<?> lowerClass = oneLower ? oneType : twoType;
        final Class<?> upperClass = oneLower ? twoType : oneType;

        final Type lowerType = oneLower ? one : two;
        final Type upperType = oneLower ? two : one;

        final Map<String, Type> lowerGenerics = GenericsResolutionUtils.resolveGenerics(lowerType, ignoreVars);
        final Map<String, Type> upperGenerics;

        if (lowerType.equals(upperType)) {
            upperGenerics = GenericsResolutionUtils.resolveGenerics(upperType, ignoreVars);
        } else {
            // resolve upper class hierarchy to get lower type generics
            upperGenerics = GenericsResolutionUtils.resolve(upperClass,
                    GenericsResolutionUtils.resolveGenerics(upperType, ignoreVars),
                    Collections.<Class<?>, LinkedHashMap<String, Type>>emptyMap(),
                    Collections.<Class<?>>emptyList())
                    .get(lowerClass);
        }
        for (Map.Entry<String, Type> entry : lowerGenerics.entrySet()) {
            final String generic = entry.getKey();
            walk(entry.getValue(), upperGenerics.get(generic), visitor);
        }
    }

    private static boolean isCompatible(final Class<?> one, final Class<?> two) {
        return one.isAssignableFrom(two) || two.isAssignableFrom(one);
    }

    private static Type arrayType(final Type type) {
        return type instanceof GenericArrayType
                ? ((GenericArrayType) type).getGenericComponentType()
                : ((Class) type).getComponentType();
    }

}

package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;
import ru.vyarus.java.generics.resolver.util.walk.ComparatorTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.CompatibilityTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.TypesWalker;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Generic-agnostic type operations.
 *
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
public final class TypeUtils {

    private TypeUtils() {
    }

    /**
     * Checks if type is more specific than provided one. E.g. {@code ArrayList} is more specific then
     * {@code List} or {@code List<Integer>} is more specific then {@code List<Object>}.
     * <p>
     * Not resolved type variables are resolved to Object.
     *
     * @param what        type to check
     * @param comparingTo type to compare to
     * @return true when provided type is more specific than other type. false otherwise
     * @throws IllegalArgumentException when types are not compatible
     */
    public static boolean isMoreSpecific(final Type what, final Type comparingTo) {
        final ComparatorTypesVisitor visitor = new ComparatorTypesVisitor();
        TypesWalker.walk(what, comparingTo, visitor);

        final IgnoreGenericsMap ignoreVars = new IgnoreGenericsMap();
        if (!visitor.isCompatible()) {
            throw new IllegalArgumentException(String.format(
                    "Type %s can't be compared to %s because they are not compatible",
                    TypeToStringUtils.toStringType(what, ignoreVars),
                    TypeToStringUtils.toStringType(comparingTo, ignoreVars)));
        }
        return visitor.isMoreSpecific();
    }

    /**
     * Not resolved type variables are resolved to Object.
     *
     * @param one first type
     * @param two second type
     * @return more specific type or first type is they are equal
     * @see #isMoreSpecific(Type, Type)
     */
    public static Type getMoreSpecificType(final Type one, final Type two) {
        return isMoreSpecific(one, two) ? one : two;
    }


    /**
     * Check if types are compatible: types must be equal or one extend another. Object is compatible with any type.
     * <p>
     * Not resolved type variables are resolved to Object.
     *
     * @param one first type
     * @param two second type
     * @return true if types are alignable, false otherwise
     */
    public static boolean isCompatible(final Type one, final Type two) {
        final CompatibilityTypesVisitor visitor = new CompatibilityTypesVisitor();
        TypesWalker.walk(one, two, visitor);
        return visitor.isCompatible();
    }


    /**
     * <pre>{@code class Owner<T> {
     *    class Inner {
     *      T field; // parent generic reference
     *    }
     * }}</pre>.
     *
     * @param type class to check
     * @return true when type is inner (requires outer class instance for creation), false otherwise
     */
    public static boolean isInner(final Type type) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getOwnerType() != null;
        }
        final Class<?> actual = GenericsUtils.resolveClass(type, new IgnoreGenericsMap());
        // interface is always static and can't use outer generics
        return !actual.isInterface() && actual.isMemberClass() && !Modifier.isStatic(actual.getModifiers());
    }

    /**
     * @param type inner class (probably)
     * @return outer class or null if type is not inner class
     */
    public static Class<?> getOuter(final Type type) {
        if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getOwnerType();
        }
        return isInner(type)
                ? GenericsUtils.resolveClass(type, new IgnoreGenericsMap()).getEnclosingClass()
                : null;
    }
}

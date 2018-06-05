package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;
import ru.vyarus.java.generics.resolver.util.walk.AssignabilityTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.ComparatorTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.CompatibilityTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.TypesWalker;

import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic-agnostic type operations.
 *
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
public final class TypeUtils {

    private static final Map<String, Type> IGNORE_VARS = IgnoreGenericsMap.getInstance();

    @SuppressWarnings({"checkstyle:Indentation", "PMD.NonStaticInitializer", "PMD.AvoidUsingShortType"})
    private static final Map<Class, Class> PRIMITIVES = new HashMap<Class, Class>() {{
        put(boolean.class, Boolean.class);
        put(byte.class, Byte.class);
        put(char.class, Character.class);
        put(double.class, Double.class);
        put(float.class, Float.class);
        put(int.class, Integer.class);
        put(long.class, Long.class);
        put(short.class, Short.class);
        put(void.class, Void.class);
    }};

    private TypeUtils() {
    }

    /**
     * @param type class to wrap
     * @return primitive wrapper class if provided class is primitive or class itself
     */
    public static Class<?> wrapPrimitive(final Class<?> type) {
        return type.isPrimitive() ? PRIMITIVES.get(type) : type;
    }

    /**
     * Checks if type is more specific than provided one. E.g. {@code ArrayList} is more specific then
     * {@code List} or {@code List<Integer>} is more specific then {@code List<Object>}.
     * <p>
     * Note that comparison logic did not follow all java rules (especially wildcards) as some rules
     * are not important at runtime.
     * <p>
     * Not resolved type variables are resolved to Object. Object is considered as unknown type: everything
     * is assignable to Object and Object is assignable to everything.
     * {@code List == List<Object> == List<?> == List<? super Object> == List<? extends Object>}.
     * <p>
     * For lower bounded wildcards more specific wildcard contains lower bound: {@code ? extends Number} is
     * more specific then {@code ? extends Integer}. Also, lower bounded wildcard is always more specific then
     * Object.
     * <p>
     * Not the same as {@link #isAssignable(Type, Type)}. For example:
     * {@code isAssignable(List, List<String>) == true}, but {@code isMoreSpecific(List, List<String>) == false}.
     * <p>
     * Primitive types are checked as wrappers (for example, int is more specific then Number).
     *
     * @param what        type to check
     * @param comparingTo type to compare to
     * @return true when provided type is more specific than other type. false otherwise
     * @throws IllegalArgumentException when types are not compatible
     * @see ComparatorTypesVisitor for implementation details
     */
    public static boolean isMoreSpecific(final Type what, final Type comparingTo) {
        final ComparatorTypesVisitor visitor = new ComparatorTypesVisitor();
        TypesWalker.walk(what, comparingTo, visitor);

        if (!visitor.isCompatible()) {
            throw new IllegalArgumentException(String.format(
                    "Type %s can't be compared to %s because they are not compatible",
                    TypeToStringUtils.toStringType(what, IGNORE_VARS),
                    TypeToStringUtils.toStringType(comparingTo, IGNORE_VARS)));
        }
        return visitor.isMoreSpecific();
    }

    /**
     * Checks if type could be casted to type. Generally, method is supposed to answer: if this type could be tried
     * to set on that place (defined by second type). Not resolved type variables are resolved to Object. Object is
     * considered as unknown type: everything is assignable to Object and Object is assignable to everything.
     * Note that {@code T<String, Object>} is assignable to {@code T<String, String>} as Object considered as unknown
     * type and so could be compatible (in opposite way is also assignable as anything is assignable to Object).
     * <p>
     * Of course, actual value used instead of Object may be incompatible, but method intended only to
     * check all available types information (if nothing stops me yet). Use exact types to get more correct
     * result. Example usage scenario: check field type before trying to assign something with reflection.
     * <p>
     * Java wildcard rules are generally not honored because at runtime they are meaningless.
     * {@code List == List<Object> == List<? super Object> == List<? extends Object>}. All upper bounds are used for
     * comparison (multiple upper bounds in wildcard could be from repackaging of generic declaration
     * {@code T<extends A&B>}. Lower bounds are taken into account as: if both have lower bound then
     * right's type bound must be higher ({@code ? extends Number and ? extends Integer}). If only left
     * type is lower bounded wildcard then it is not assignable (except Object).
     * <p>
     * Primitive types are checked as wrappers (for example, int is more specific then Number).
     *
     * @param what   type to check
     * @param toType type to check assignability for
     * @return true if types are equal or type is more specific, false if can't be casted or types incompatible
     * @see AssignabilityTypesVisitor for implementation details
     */
    public static boolean isAssignable(final Type what, final Type toType) {
        final AssignabilityTypesVisitor visitor = new AssignabilityTypesVisitor();
        TypesWalker.walk(what, toType, visitor);

        return visitor.isAssignable();
    }

    /**
     * Method is useful for wildcards processing. Note that it is impossibel in java to have multiple types in wildcard,
     * but generics resolver use wildcards to store multiple generic bounds from raw resolution
     * ({@code T extends Something & Comparable} stored as wildcard {@code ? extends Something & Comparable}).
     * Use only when exact precision is required, otherwise you can use just first classes from both
     * (first upper bound) as multiple bounds case is quite rare.
     * <p>
     * Bounds are assignable if one class from left bound is assignable to all types in right bound.
     * For example,
     * <ul>
     * <li>{@code Number & Serializable and Number} assignable</li>
     * <li>{@code Integer & Serializable and Number & Comparable} assignable</li>
     * <li>{@code Integer and Number & Serializable} not assignable</li>
     * </ul>
     * <p>
     * Object is assumed as unknown type. Object could be assigned to any type and any type could be assigned to
     * Object. Closest analogy from java rules is no generics: for example, {@code List} without generics could be
     * assigned anywhere ({@code List<Integer> = List} - valid java code).
     * <p>
     * No primitives expected: no special wrapping performed (as supplement method
     * {@link GenericsUtils#resolveUpperBounds(Type, Map)} already handle primitives).
     *
     * @param one first bound
     * @param two second bound
     * @return true if left bound could be assigned to right bound, false otherwise
     * @see GenericsUtils#resolveUpperBounds(Type, Map) supplement resolution method
     */
    @SuppressWarnings("PMD.UseVarargs")
    public static boolean isAssignableBounds(final Class[] one, final Class[] two) {
        if (one.length == 0 || two.length == 0) {
            throw new IllegalArgumentException(String.format("Incomplete bounds information: %s %s",
                    Arrays.toString(one), Arrays.toString(two)));
        }
        for (Class<?> oneType : one) {
            boolean assignable = true;
            for (Class<?> twoType : two) {
                // objects are unknown types - assuming assignable
                if (oneType != Object.class && twoType != Object.class
                        && !twoType.isAssignableFrom(oneType)) {
                    assignable = false;
                    break;
                }
            }
            if (assignable) {
                // found one type assignable to all right types
                return true;
            }
        }
        return false;
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
     * <p>
     * Primitive types are checked as wrappers (for example, int is more specific then Number).
     *
     * @param one first type
     * @param two second type
     * @return true if types are alignable, false otherwise
     * @see TypesWalker for implementation details
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
        final Class<?> actual = GenericsUtils.resolveClass(type, IGNORE_VARS);
        // interface is always static and can't use outer generics
        return !actual.isInterface() && actual.isMemberClass() && !Modifier.isStatic(actual.getModifiers());
    }

    /**
     * May return {@link ParameterizedType} if incoming type contains owner declaration like
     * {@code Outer<String>.Inner field} ({@code field.getGenericType()} will contain outer generic).
     *
     * @param type inner class (probably)
     * @return outer type or null if type is not inner class
     */
    public static Type getOuter(final Type type) {
        if (type instanceof ParameterizedType) {
            // could contain outer generics
            return ((ParameterizedType) type).getOwnerType();
        }
        return isInner(type)
                ? GenericsUtils.resolveClass(type, IGNORE_VARS).getEnclosingClass()
                : null;
    }
}

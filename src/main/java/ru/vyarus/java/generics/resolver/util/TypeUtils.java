package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException;
import ru.vyarus.java.generics.resolver.util.type.CommonTypeFactory;
import ru.vyarus.java.generics.resolver.util.type.InstanceTypeFactory;
import ru.vyarus.java.generics.resolver.util.walk.AssignabilityTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.ComparatorTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.CompatibilityTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.TypesWalker;

import java.lang.reflect.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Types utilities.
 *
 * @author Vyacheslav Rusakov
 * @see ArrayTypeUtils for array specific utilities
 * @since 13.05.2018
 */
public final class TypeUtils {

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
     * @return true when provided type is more specific than other type,
     * false otherwise (including when types are equal)
     * @throws IncompatibleTypesException when types are not compatible
     * @see ComparatorTypesVisitor for implementation details
     * @see #isCompatible(Type, Type) use for compatibility check (before) to avoid incompatible types exception
     * @see #isMoreSpecificOrEqual(Type, Type) for broader check
     */
    public static boolean isMoreSpecific(final Type what, final Type comparingTo) {
        if (what.equals(comparingTo)) {
            // assume correct type implementation (for faster check)
            return false;
        }
        return doMoreSpecificWalk(what, comparingTo).isMoreSpecific();
    }

    /**
     * Shortcut for {@link #isMoreSpecific(Type, Type)} for cases when equality is also acceptable (quite often
     * required case).
     *
     * @param what        type to check
     * @param comparingTo type to compare to
     * @return true when provided type is more specific than other type or equal, false otherwise
     * @throws IncompatibleTypesException when types are not compatible
     */
    public static boolean isMoreSpecificOrEqual(final Type what, final Type comparingTo) {
        if (what.equals(comparingTo)) {
            // assume correct type implementation (for faster check)
            return true;
        }
        final ComparatorTypesVisitor visitor = doMoreSpecificWalk(what, comparingTo);
        return visitor.isMoreSpecific() || visitor.isEqual();
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
        if (what.equals(toType)) {
            // assume correct type implementation (for faster check)
            return true;
        }
        final AssignabilityTypesVisitor visitor = new AssignabilityTypesVisitor();
        TypesWalker.walk(what, toType, visitor);

        return visitor.isAssignable();
    }

    /**
     * Method is useful for wildcards processing. Note that it is impossible in java to have multiple types in wildcard,
     * but generics resolver use wildcards to store multiple generic bounds from raw resolution
     * ({@code T extends Something & Comparable} stored as wildcard {@code ? extends Something & Comparable}).
     * Use only when exact precision is required, otherwise you can use just first classes from both
     * (first upper bound) as multiple bounds case is quite rare.
     * <p>
     * Bounds are assignable if all classes in right bound are assignable from any class in left bound.
     * For example,
     * <ul>
     * <li>{@code Number & SomeInterface and Number} assignable</li>
     * <li>{@code Integer & SomeInterface and Number & Comparable} assignable</li>
     * <li>{@code Integer and Number & SomeInterface} not assignable</li>
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
     * @see GenericsUtils#resolveUpperBounds(Type, Map) supplement bound resolution method
     */
    @SuppressWarnings({"PMD.UseVarargs", "PMD.CyclomaticComplexity", "checkstyle:CyclomaticComplexity"})
    public static boolean isAssignableBounds(final Class[] one, final Class[] two) {
        if (one.length == 0 || two.length == 0) {
            throw new IllegalArgumentException(String.format("Incomplete bounds information: %s %s",
                    Arrays.toString(one), Arrays.toString(two)));
        }
        // nothing to do for Object - it's assignable to anything
        if (!(one.length == 1 && (one[0] == Object.class
                || (ArrayTypeUtils.isArray(one[0]) && ArrayTypeUtils.getArrayComponentType(one[0]) == Object.class)))) {
            for (Class<?> twoType : two) {
                // everything is assignable to object
                if (twoType != Object.class) {
                    boolean assignable = false;
                    for (Class<?> oneType : one) {
                        if (twoType.isAssignableFrom(oneType)) {
                            assignable = true;
                            break;
                        }
                    }
                    // none on left types is assignable to this right type
                    if (!assignable) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    /**
     * Not resolved type variables are resolved to Object.
     *
     * @param one first type
     * @param two second type
     * @return more specific type or first type if they are equal
     * @throws IncompatibleTypesException when types are not compatible
     * @see #isMoreSpecific(Type, Type)
     */
    public static Type getMoreSpecificType(final Type one, final Type two) {
        return isMoreSpecificOrEqual(one, two) ? one : two;
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
        final Class<?> actual = GenericsUtils.resolveClassIgnoringVariables(type);
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
                ? GenericsUtils.resolveClassIgnoringVariables(type).getEnclosingClass()
                : null;
    }

    /**
     * Searching for maximum type to which both types could be downcasted. For example, for {@code Integer}
     * and {@code Double} common class would be {@code ? extends Number & Comparable<Number>} (because both
     * {@code Integer} and {@code Double} extend {@code Number} and implement {@code Comparable} and so both types
     * could be downcasted to this common type).
     * <p>
     * Generics are also counted, e.g. common type for {@code List<Double>} and {@code List<Integer>} is
     * {@code List<Number & Comparable<Number>>}. Generics are tracked on all depths (in order to compute maximally
     * accurate type).
     * <p>
     * Types may be common only in implemented interfaces: e.g. for {@code class One implements Comparable}
     * and {@code class Two implements Comparable} common type would be {@code Comparable}.
     * All shared interfaces are returned as wildcard ({@link WildcardType} meaning
     * {@code ? extends BaseType & IFace1 & Iface2 }).
     * <p>
     * For returned wildcard, contained types will be sorted as: class, interface from not java package, interface
     * with generic(s), by name. So order of types will always be predictable and first upper bound will be the most
     * specific type.
     * <p>
     * Returned type is maximally accurate common type, but if you need only base class without interfaces
     * (and use interfaces only if direct base class can't be found) then you can call
     * {@code CommonTypeFactory.build(one, two, false)} directly.
     * <p>
     * Primitives are boxed for comparison (e.g. {@code int -> Integer}) so even for two {@code int} common type
     * would be {@code Integer}. The exception is primitive arrays: they can't be unboxed (because {@code int[]}
     * and {@code Integer[]} are not the same) and so the result will be {@code Object} for primitive arrays in
     * all cases except when types are equal.
     * <p>
     * NOTE: returned type will not contain variables ({@link TypeVariable}), even if provided types contain them
     * (all variables are solved to upper bound). For example, {@code List<T extends Number>} will be counted as
     * {@code List<Number>} and {@code Set<N>} as {@code Set<Object>}.
     *
     * @param one first type
     * @param two second type
     * @return maximum class assignable to both types or {@code Object} if classes are incompatible
     */
    public static Type getCommonType(final Type one, final Type two) {
        return CommonTypeFactory.build(one, two, true);
    }

    /**
     * Analyze provided instance and return instance type. In the simplest case it would be just
     * {@code instance.getClass()}, but with class generics resolved by upper bounds. In case of multiple instances
     * provided, it would be median type (type assignable from all instances).
     * <p>
     * The essential part is that returned type would contain original instance(s)
     * ({@link ru.vyarus.java.generics.resolver.util.type.instance.InstanceType}). So even if all type information
     * can't be resolved right now from provided instance(s), it could be done later (when some logic, aware
     * of instance types could detect it and analyze instance further). For example, container types like
     * {@link java.util.List}: initially only list class could be extracted form list instance, but logic
     * knowing what list is and so be able to extract instances can further analyze list content and so resolve
     * list generic type:
     * <pre>{@code
     * List listInstance = [12, 13.4] // integer and double
     * Type type = TypeUtils.getInstanceType(listInstance);      // List<Object>
     *
     * // somewhere later where we know how to work with a list
     * if (List.class.isAssignableFrom(GenericUtils.resolveClass(type))
     *                                  && type instanceof InstanceType) {
     *     ParameterizedInstanceType ptype = type.getImprovableType();  // will be the same instance
     *
     *     assert ptype.isCompleteType() == false // type not yet completely resolved
     *
     *     List containedList = (List) ptype.getInstance()
     *     Type innerType = TypeUtils.getInstanceType(containedList.toArray()) // ? extends Number & Comparable<Number>
     *
     *     // improving list type: type changes form List<Object> to List<Number & Comparable<Number>>
     *     ptype.improveAccuracy(innerType)
     *     // note that we changing the original instance type so application will use more complete type in all
     *     // places where this type were referenced
     *
     *     assert ptype.isCompleteType() == true // type considered complete now (completely resolved)
     * }
     * }</pre>
     * <p>
     * Instance types behave as usual types and so could be used in any api required types. The only difference
     * is contained instance so logic parts aware for instance types could detect them and use instance for
     * type improvement.
     * <p>
     * Note that returned instance type will contain only not null instances. If all instances are null then
     * returned type will be simply {@code Object}, because object is assignable to anything (at least
     * {@link TypeUtils} methods assume that).
     * <p>
     * Also, if instance is an array, which does not contain non null instances, then simple array class
     * is returned (e.g. {@code TypeUtils.getInstanceType(new Integer[0]) == Integer[].class}
     *
     * @param instances instances to resolve type from
     * @return instance type for one instance and median type for multiple instances
     * @see ru.vyarus.java.generics.resolver.util.type.instance.InstanceType for more info
     */
    public static Type getInstanceType(final Object... instances) {
        return InstanceTypeFactory.build(instances);
    }

    private static ComparatorTypesVisitor doMoreSpecificWalk(final Type what, final Type comparingTo) {
        final ComparatorTypesVisitor visitor = new ComparatorTypesVisitor();
        TypesWalker.walk(what, comparingTo, visitor);

        if (!visitor.isCompatible()) {
            throw new IncompatibleTypesException("Type %s can't be compared to %s because they are not compatible",
                    what, comparingTo);
        }
        return visitor;
    }
}

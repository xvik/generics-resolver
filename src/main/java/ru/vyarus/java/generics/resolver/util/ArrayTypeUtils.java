package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * Utilities for working with array types.
 *
 * @author Vyacheslav Rusakov
 * @since 16.03.2019
 */
public final class ArrayTypeUtils {

    private static final String ARRAY_TYPE_SIMPLE_PREFIX = "[";
    private static final String ARRAY_TYPE_OBJECT_PREFIX = "[L";

    @SuppressWarnings({"checkstyle:Indentation", "PMD.NonStaticInitializer", "PMD.AvoidUsingShortType"})
    private static final Map<Class, String> PRIMITIVE_ARRAY_LETTER = new HashMap<Class, String>() {{
        put(byte.class, "B");
        put(char.class, "C");
        put(double.class, "D");
        put(float.class, "F");
        put(int.class, "I");
        put(long.class, "J");
        put(short.class, "S");
        put(boolean.class, "Z");
    }};

    private ArrayTypeUtils() {
    }

    /**
     * @param type type to check
     * @return true if type is array or generic array ({@link GenericArrayType}), false otherwise.
     */
    public static boolean isArray(final Type type) {
        return (type instanceof Class && ((Class) type).isArray()) || type instanceof GenericArrayType;
    }

    /**
     * For example, {@code getArrayComponentType(int[]) == int} and
     * {@code getArrayComponentType(List<String>[]) == List<String>}.
     *
     * @param type array type (class or {@link GenericArrayType}
     * @return array component type
     * @throws IllegalArgumentException if provided type is not array
     * @see #isArray(Type)
     */
    public static Type getArrayComponentType(final Type type) {
        if (!isArray(type)) {
            throw new IllegalArgumentException("Provided type is not an array: "
                    + TypeToStringUtils.toStringType(type));
        }
        if (type instanceof GenericArrayType) {
            return ((GenericArrayType) type).getGenericComponentType();
        } else {
            return ((Class) type).getComponentType();
        }
    }

    /**
     * Returns array class for provided class. For example, {@code toArrayClass(int) == int[]} and
     * {@code toArrayClass(List) == List[]}.
     * <p>
     * Pay attention that primitive arrays are returned for primitive types (no boxing applied automatically).
     * Also, primitives are not playing well with generalization, so you can't write
     * {@code Class<int[]> res = ArrayTypeUtils.toArrayClass(int.class)} (because IDE assumes {@code Integer[]} as
     * the return type, whereas actually {@code int[].class} will be returned). Use instead:
     * {@code Class<?> res = ArrayTypeUtils.toArrayClass(int.class)} or {@link #toArrayType(Type)} method.
     *
     * @param type class to get array of
     * @param <T>  parameter type
     * @return class representing array of provided type
     * @see <a href="https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-4.html#jvms-4.3">jvm arrays spec</a>
     * @see #toArrayType(Type) for general array case
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T[]> toArrayClass(final Class<T> type) {
        try {
            final String name = type.getName();
            final String typeName;
            if (type.isArray()) {
                typeName = ARRAY_TYPE_SIMPLE_PREFIX + name;
            } else if (type.isPrimitive()) {
                typeName = ARRAY_TYPE_SIMPLE_PREFIX + PRIMITIVE_ARRAY_LETTER.get(type);
            } else {
                typeName = ARRAY_TYPE_OBJECT_PREFIX + name + ";";
            }
            return (Class<T[]>) Class.forName(typeName);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Failed to create array class for " + type.getSimpleName(), e);
        }
    }

    /**
     * There are two possible cases:
     * <ul>
     * <li>Type is pure class - then arrays is simple class (e.g. {@code int[].class} or {@code List[].class}) </li>
     * <li>Type is generified - then {@link GenericArrayType} must be used (e.g. for parameterized type
     * {@code List<String>})</li>
     * </ul>
     *
     * @param type type to get array of
     * @return array type
     * @see #toArrayClass(Class) for pure class case
     */
    public static Type toArrayType(final Type type) {
        return type instanceof Class ? toArrayClass((Class<?>) type)
                : new GenericArrayTypeImpl(type);
    }
}

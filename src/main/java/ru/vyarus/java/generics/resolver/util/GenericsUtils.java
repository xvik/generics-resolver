package ru.vyarus.java.generics.resolver.util;

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Helper utilities to correctly resolve generified types of super interfaces.
 *
 * @author Vyacheslav Rusakov
 * @since 17.10.2014
 */
public final class GenericsUtils {
    private GenericsUtils() {
    }

    /**
     * Called to properly resolve return type of root finder or inherited finder method.
     * Supposed to return enough type info to detect return type (collection, array or plain object).
     *
     * @param method   method to analyze
     * @param generics generics resolution map for method class (will be null for root)
     * @return return type class
     */
    public static Class<?> getReturnClass(final Method method, final Map<String, Type> generics) {
        final Type returnType = method.getGenericReturnType();
        return resolveClass(returnType, generics);
    }

    /**
     * Resolve generics in method parameters.
     *
     * @param method   method to resolve parameters
     * @param generics type generics
     * @return resolved method parameter types
     */
    public static List<Class<?>> getMethodParameters(final Method method, final Map<String, Type> generics) {
        final List<Class<?>> params = new ArrayList<Class<?>>();
        for (Type type : method.getGenericParameterTypes()) {
            params.add(resolveClass(type, generics));
        }
        return params;
    }

    /**
     * Called to properly resolve generified type (e.g. generified method return).
     * For example, when calling for {@code List<T>} it will return type of {@code T}.
     *
     * @param type     type to analyze
     * @param generics root class generics mapping
     * @return resolved generic classes
     * @throws NoGenericException when generic not found or not generified type provided
     */
    public static List<Class<?>> resolveGenericsOf(final Type type,
                                                   final Map<String, Type> generics) throws NoGenericException {
        final List<Class<?>> res = new ArrayList<Class<?>>();
        Type analyzingType = type;
        if (type instanceof TypeVariable) {
            // if type is pure generic recovering parametrization
            analyzingType = generics.get(((TypeVariable) type).getName());
        }
        if (!(analyzingType instanceof ParameterizedType)
                || ((ParameterizedType) analyzingType).getActualTypeArguments().length == 0) {
            throw new NoGenericException();
        } else {
            final Type[] actualTypeArguments = ((ParameterizedType) analyzingType).getActualTypeArguments();
            for (final Type actual : actualTypeArguments) {
                if (actual instanceof Class) {
                    res.add((Class) actual);
                } else {
                    // deep generics resolution required
                    res.add(resolveClass(generics.get(((TypeVariable) actual).getName()), generics));
                }
            }
        }
        return res;
    }

    /**
     * Resolves top class for provided type (for example, for generified classes like {@code List<T>} it
     * returns base type List).
     *
     * @param type     type to resolve
     * @param generics root class generics mapping
     * @return resolved class
     */
    public static Class<?> resolveClass(final Type type, final Map<String, Type> generics) {
        Class<?> res;
        if (type instanceof Class) {
            res = (Class) type;
        } else if (type instanceof ParameterizedType) {
            res = resolveClass(((ParameterizedType) type).getRawType(), generics);
        } else if (type instanceof TypeVariable) {
            res = resolveClass(generics.get(((TypeVariable) type).getName()), generics);
        } else if (type instanceof WildcardType) {
            final Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            res = resolveClass(upperBounds[0], generics);
        } else {
            final Class arrayType = resolveClass(((GenericArrayType) type).getGenericComponentType(), generics);
            try {
                // returning complete array class with resolved type
                if (arrayType.isArray()) {
                    res = Class.forName("[" + arrayType.getName());
                } else {
                    res = Class.forName("[L" + arrayType.getName() + ";");
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("Failed to create array class", e);
            }
        }
        return res;
    }
}

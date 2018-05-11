package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;

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
     * @throws NoGenericException      when generic not found or not generified type provided
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     */
    public static List<Class<?>> resolveGenericsOf(final Type type,
                                                   final Map<String, Type> generics) throws NoGenericException {
        final List<Class<?>> res = new ArrayList<Class<?>>();
        Type analyzingType = type;
        if (type instanceof TypeVariable) {
            // if type is pure generic recovering parametrization
            analyzingType = declaredGeneric((TypeVariable) type, generics);
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
                    res.add(resolveClass(actual, generics));
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
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     */
    public static Class<?> resolveClass(final Type type, final Map<String, Type> generics) {
        final Class<?> res;
        if (type instanceof Class) {
            res = (Class) type;
        } else if (type instanceof ParameterizedType) {
            res = resolveClass(((ParameterizedType) type).getRawType(), generics);
        } else if (type instanceof TypeVariable) {
            res = resolveClass(declaredGeneric((TypeVariable) type, generics), generics);
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

    /**
     * Resolve type generics. Returned type will contain actual types instead of generic names. Most likely, returned
     * type will be different than provided: for example, original type may be {@link TypeVariable} and returned
     * will be simple {@link Class} (resolved generic value).
     *
     * @param type     type to resolve
     * @param generics root class generics mapping
     * @return resolved type
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     */
    public static Type resolveTypeVariables(final Type type, final Map<String, Type> generics) {
        Type resolvedGenericType = null;
        if (type instanceof TypeVariable) {
            // simple named generics resolved to target types
            resolvedGenericType = declaredGeneric((TypeVariable) type, generics);
        } else if (type instanceof Class) {
            resolvedGenericType = type;
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parametrizedType = (ParameterizedType) type;
            resolvedGenericType = new ParameterizedTypeImpl(parametrizedType.getRawType(),
                    resolve(parametrizedType.getActualTypeArguments(), generics), parametrizedType.getOwnerType());
        } else if (type instanceof GenericArrayType) {
            final GenericArrayType arrayType = (GenericArrayType) type;
            resolvedGenericType = new GenericArrayTypeImpl(resolveTypeVariables(
                    arrayType.getGenericComponentType(), generics));
        } else if (type instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) type;
            resolvedGenericType = new WildcardTypeImpl(resolve(wildcard.getUpperBounds(), generics),
                    resolve(wildcard.getLowerBounds(), generics));
        }
        return resolvedGenericType;
    }

    private static Type[] resolve(final Type[] types, final Map<String, Type> generics) {
        final Type[] resolved = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = resolveTypeVariables(types[i], generics);
        }
        return resolved;
    }

    private static Type declaredGeneric(final TypeVariable generic, final Map<String, Type> declarations) {
        final String name = generic.getName();
        final Type result = declarations.get(name);
        if (result == null) {
            throw new UnknownGenericException(name);
        }
        return result;
    }
}

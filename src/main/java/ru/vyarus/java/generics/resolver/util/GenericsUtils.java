package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;
import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;
import ru.vyarus.java.generics.resolver.error.UnknownGenericException;

import java.lang.reflect.*;
import java.util.*;

/**
 * Helper utilities to correctly resolve generified types of super interfaces.
 *
 * @author Vyacheslav Rusakov
 * @since 17.10.2014
 */
@SuppressWarnings("PMD.GodClass")
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
     * Resolve named variables in method parameters types.
     *
     * @param method   method to resolve parameters
     * @param generics type generics
     * @return resolved method parameter types without named generics
     */
    public static List<Type> getMethodParametersTypes(final Method method, final Map<String, Type> generics) {
        final List<Type> params = new ArrayList<Type>();
        for (Type type : method.getGenericParameterTypes()) {
            params.add(resolveTypeVariables(type, generics));
        }
        return params;
    }

    /**
     * Called to properly resolve generified type (e.g. generified method return).
     * For example, when calling for {@code List<T>} it will return type of {@code T}.
     *
     * @param type     type to analyze
     * @param generics root class generics mapping
     * @return resolved generic classes or empty list if type does not support generics
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     */
    public static List<Class<?>> resolveGenericsOf(final Type type, final Map<String, Type> generics) {
        final List<Class<?>> res = new ArrayList<Class<?>>();
        Type analyzingType = type;
        if (type instanceof TypeVariable) {
            // if type is pure generic recovering parametrization
            analyzingType = declaredGeneric((TypeVariable) type, generics);
        }
        if ((analyzingType instanceof ParameterizedType)
                && ((ParameterizedType) analyzingType).getActualTypeArguments().length > 0) {
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
        } else if (type instanceof ExplicitTypeVariable) {
            res = resolveClass(((ExplicitTypeVariable) type).getBounds()[0], generics);
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
                throw new IllegalStateException("Failed to create array class for " + arrayType.getSimpleName(), e);
            }
        }
        return res;
    }

    /**
     * In most cases {@link #resolveClass(Type, Map)} could be used instead (for simplicity). This method will
     * only return different result for wildcards inside resolved types (where generics are replaced
     * {@link #resolveTypeVariables(Type, Map)}). Also, in contrast to {@link #resolveClass(Type, Map)},
     * method will replace primitive types with wrappers (int -> Integer etc.) because this method is used mostly for
     * comparison logic and avoiding primitives simplifies it.
     * <p>
     * Wildcards are used to store raw resolution of generic declaration {@code T extends Number & Comparable}
     * (but {@code ? extends Number & Comparable} is not allowed in java). Only for this case multiple bounds
     * will be returned.
     * <p>
     * That precision may be important only for exact types compatibility logic.
     *
     * @param type     type to resolve upper bounds
     * @param generics known generics
     * @return resolved upper bounds (at least one class)
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see TypeUtils#isAssignableBounds(Class[], Class[]) supplement check method
     */
    public static Class[] resolveUpperBounds(final Type type, final Map<String, Type> generics) {
        final Class[] res;
        if (type instanceof WildcardType) {
            final List<Class> list = new ArrayList<Class>();
            for (Type t : ((WildcardType) type).getUpperBounds()) {
                final Class<?> bound = resolveClass(t, generics);
                // possible case: T extends K & Serializable - if T unknown then it become
                // T extends Object & Serializable
                if (bound != Object.class) {
                    list.add(bound);
                }
            }
            if (list.isEmpty()) {
                list.add(Object.class);
            }
            res = list.toArray(new Class[0]);
        } else {
            // get rid of primitive to simplify comparison logic
            res = new Class[]{TypeUtils.wrapPrimitive(resolveClass(type, generics))};
        }
        return res;
    }

    /**
     * Resolve type generics. Returned type will contain actual types instead of generic names. Most likely, returned
     * type will be different than provided: for example, original type may be {@link TypeVariable} and returned
     * will be simple {@link Class} (resolved generic value).
     * <p>
     * Note that upper bounded wildcards are flattened to simple type ({@code ? extends Somthing -> Something} as
     * upper bounded wildcards are not useful at runtime. The only exception is wildcard with multiple bounds
     * (repackaged declaration {@code T extends A&B}). Wildcards with Object as bound are also flattened
     * ({@code List<? extends Object> --> List<Object>}, {@code List<?> --> List<Object>},
     * {@code List<? super Object> --> List<Object>}.
     *
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
        } else if (type instanceof ExplicitTypeVariable) {
            // special type used to preserve named generic (and differentiate from type variable)
            resolvedGenericType = type;
        } else if (type instanceof Class) {
            resolvedGenericType = type;
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parametrizedType = (ParameterizedType) type;
            resolvedGenericType = new ParameterizedTypeImpl(parametrizedType.getRawType(),
                    resolveTypeVariables(parametrizedType.getActualTypeArguments(), generics),
                    parametrizedType.getOwnerType());
        } else if (type instanceof GenericArrayType) {
            final GenericArrayType arrayType = (GenericArrayType) type;
            resolvedGenericType = new GenericArrayTypeImpl(resolveTypeVariables(
                    arrayType.getGenericComponentType(), generics));
        } else if (type instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) type;
            if (wildcard.getLowerBounds().length > 0) {
                // only one lower bound could be (? super A)
                final Type lowerBound = resolveTypeVariables(wildcard.getLowerBounds(), generics)[0];
                // flatten <? super Object> to Object
                resolvedGenericType = lowerBound == Object.class
                        ? Object.class : WildcardTypeImpl.lower(lowerBound);
            } else {
                // could be multiple upper bounds because of named generic bounds repackage (T extends A & B)
                final Type[] upperBounds = resolveTypeVariables(wildcard.getUpperBounds(), generics);
                // flatten <? extends Object> (<?>) to Object and <? extends Something> to Something
                resolvedGenericType = upperBounds.length == 1 ? upperBounds[0] : WildcardTypeImpl.upper(upperBounds);
            }
        }
        return resolvedGenericType;
    }

    /**
     * Shortcut for {@link #resolveTypeVariables(Type, Map)} to process multiple types at once.
     *
     * @param types    types to replace named generics in
     * @param generics known generics
     * @return types without named generics
     */
    public static Type[] resolveTypeVariables(final Type[] types, final Map<String, Type> generics) {
        final Type[] resolved = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = resolveTypeVariables(types[i], generics);
        }
        return resolved;
    }

    /**
     * It is important to keep possible outer class generics, because they may be used in type declarations.
     *
     * @param type     type
     * @param generics all type's context generics (self + outer class)
     * @return owner class generics if type is inner class or empty map if not
     */
    public static Map<String, Type> extractOwnerGenerics(final Class<?> type,
                                                         final Map<String, Type> generics) {
        final boolean hasOwnerGenerics =
                type.isMemberClass() && type.getTypeParameters().length != generics.size();
        if (!hasOwnerGenerics) {
            return Collections.emptyMap();
        }
        final LinkedHashMap<String, Type> res = new LinkedHashMap<String, Type>(generics);
        // owner generics are all generics not mentioned in signature
        for (TypeVariable var : type.getTypeParameters()) {
            res.remove(var.getName());
        }
        return res;
    }


    /**
     * Generics declaration may contain type's generics together with outer class generics (if type is inner class).
     * Return map itself for not inner class (and if no extra generics present in map).
     *
     * @param type     type
     * @param generics all type's context generics (self + outer class)
     * @return generics, declared on type ({@code A<T, K> -> T, K}) or empty map if no generics declared on type
     */
    public static Map<String, Type> extractSelfGenerics(final Class<?> type,
                                                        final Map<String, Type> generics) {
        final boolean hasOwnerGenerics =
                type.isMemberClass() && type.getTypeParameters().length != generics.size();
        if (!hasOwnerGenerics) {
            return generics;
        }
        final LinkedHashMap<String, Type> res = new LinkedHashMap<String, Type>();
        // owner generics are all generics not mentioned in signature
        for (TypeVariable var : type.getTypeParameters()) {
            final String name = var.getName();
            res.put(name, generics.get(name));
        }
        return res;
    }

    /**
     * Filter owner generics from all generics map.
     *
     * @param generics all generics
     * @param owner    used owner type generics
     * @return type's own generics only
     * @see #extractOwnerGenerics(Class, Map)
     */
    public static Map<String, Type> filterOwnerGenerics(final Map<String, Type> generics,
                                                        final Map<String, Type> owner) {
        final Map<String, Type> res;
        if (owner.isEmpty()) {
            res = generics;
        } else {
            res = new LinkedHashMap<String, Type>(generics);
            for (String key : owner.keySet()) {
                res.remove(key);
            }
        }
        return res;
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

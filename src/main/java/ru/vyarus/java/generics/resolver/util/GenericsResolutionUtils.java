package ru.vyarus.java.generics.resolver.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generics analysis utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
// LinkedHashMap used instead of usual map to avoid accidental simple map usage (order is important!)
@SuppressWarnings("PMD.LooseCoupling")
public final class GenericsResolutionUtils {

    private static final LinkedHashMap<String, Type> EMPTY_MAP = new LinkedHashMap<String, Type>(0);
    private static final String GROOVY_OBJECT = "GroovyObject";

    private GenericsResolutionUtils() {
    }

    /**
     * Analyze class hierarchy and resolve actual generic values for all composing types.
     *
     * @param type          class to analyze
     * @param rootGenerics  resolved root type generics
     * @param knownGenerics type generics known before analysis (some middle class generics are known)
     * @param ignoreClasses interface classes to ignore during analysis
     * @return resolved generics for all types in class hierarchy
     */
    public static Map<Class<?>, LinkedHashMap<String, Type>> resolve(
            final Class<?> type,
            final LinkedHashMap<String, Type> rootGenerics,
            final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics,
            final List<Class<?>> ignoreClasses) {
        final Map<Class<?>, LinkedHashMap<String, Type>> generics =
                new HashMap<Class<?>, LinkedHashMap<String, Type>>();
        generics.put(type, rootGenerics);
        analyzeType(generics, type, knownGenerics, ignoreClasses);
        return generics;
    }


    /**
     * Resolve generics for type. Returns non empty map only if type is {@link ParameterizedType}.
     *
     * @param type     type to resolve generics for
     * @param generics generics of context class
     * @return resolved generics of parameterized type or empty map
     */
    public static LinkedHashMap<String, Type> resolveGenerics(final Type type,
                                                              final Map<String, Type> generics) {
        final LinkedHashMap<String, Type> res = new LinkedHashMap<String, Type>();
        if (type instanceof ParameterizedType) {
            final ParameterizedType actualType = (ParameterizedType) type;
            final Type[] genericTypes = actualType.getActualTypeArguments();
            final Class interfaceType = (Class) actualType.getRawType();
            final TypeVariable[] genericNames = interfaceType.getTypeParameters();

            final int cnt = genericNames.length;
            for (int i = 0; i < cnt; i++) {
                final Type resolvedGenericType = GenericsUtils.resolveTypeVariables(genericTypes[i], generics);
                res.put(genericNames[i].getName(), resolvedGenericType);
            }
        }
        return res;
    }

    /**
     * Resolve type generics by declaration (as lower bound). Used for cases when actual generic definition is not
     * available (so actual generics are unknown). In most cases such generics resolved as Object
     * (for example, {@code Some<T>}).
     *
     * @param type class to analuze generics for
     * @return resolved generics or empty map if not generics used
     */
    public static LinkedHashMap<String, Type> resolveRawGenerics(final Class<?> type) {
        final TypeVariable[] declaredGenerics = type.getTypeParameters();
        final LinkedHashMap<String, Type> generics = new LinkedHashMap<String, Type>();
        for (TypeVariable variable : declaredGenerics) {
            generics.put(variable.getName(), GenericsUtils.resolveTypeVariables(variable.getBounds()[0], generics));
        }
        return generics;
    }

    /**
     * Analyze type hierarchy (all subclasses and interfaces).
     *
     * @param generics      resolved generics of already analyzed types
     * @param knownGenerics type generics known before analysis (some middle class generics are known)
     * @param type          class to analyze
     * @param ignoreClasses interface classes to ignore during analysis
     */
    private static void analyzeType(final Map<Class<?>, LinkedHashMap<String, Type>> generics,
                                    final Class<?> type,
                                    final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics,
                                    final List<Class<?>> ignoreClasses) {
        Class<?> supertype = type;
        while (true) {
            for (Type iface : supertype.getGenericInterfaces()) {
                analyzeInterface(generics, knownGenerics, iface, supertype, ignoreClasses);
            }
            final Class next = supertype.getSuperclass();
            if (next == null || Object.class == next || ignoreClasses.contains(next)) {
                break;
            }
            // possibly provided generics (externally)
            generics.put(next, knownGenerics.containsKey(next)
                    ? knownGenerics.get(next)
                    : analyzeParent(supertype, generics.get(supertype)));
            supertype = next;
        }
    }

    /**
     * Analyze interface generics. If type is contained in known types - no generics resolution performed
     * (trust provided info).
     *
     * @param types         resolved generics of already analyzed types
     * @param knownTypes    type generics known before analysis (some middle class generics are known)
     * @param iface         interface to analyze
     * @param hostType      class implementing interface (where generics actually defined)
     * @param ignoreClasses interface classes to ignore during analysis
     */
    private static void analyzeInterface(final Map<Class<?>, LinkedHashMap<String, Type>> types,
                                         final Map<Class<?>, LinkedHashMap<String, Type>> knownTypes,
                                         final Type iface,
                                         final Class<?> hostType,
                                         final List<Class<?>> ignoreClasses) {
        final Class interfaceType = iface instanceof ParameterizedType
                ? (Class) ((ParameterizedType) iface).getRawType()
                : (Class) iface;
        if (!ignoreClasses.contains(interfaceType)) {
            if (knownTypes.containsKey(interfaceType)) {
                // check possibly already resolved generics (if provided externally)
                types.put(interfaceType, knownTypes.get(interfaceType));
            } else if (iface instanceof ParameterizedType) {
                final ParameterizedType parametrization = (ParameterizedType) iface;
                final LinkedHashMap<String, Type> generics =
                        resolveGenerics(parametrization, types.get(hostType));

                // no generics case and same resolved generics are ok (even if types in different branches of hierarchy)
                if (types.containsKey(interfaceType) && !generics.equals(types.get(interfaceType))) {
                    throw new IllegalStateException(String.format(
                            "Duplicate interface %s declaration in hierarchy: "
                                    + "can't properly resolve generics.", interfaceType.getName()));
                }
                types.put(interfaceType, generics);
            } else if (interfaceType.getTypeParameters().length > 0) {
                // root class didn't declare generics
                types.put(interfaceType, resolveRawGenerics(interfaceType));
            } else if (!GROOVY_OBJECT.equals(interfaceType.getSimpleName())) {
                // avoid groovy specific interface (all groovy objects implements it)
                types.put(interfaceType, EMPTY_MAP);
            }
            analyzeType(types, interfaceType, knownTypes, ignoreClasses);
        }
    }

    /**
     * Analyze super class generics (relative to provided type). Class may not declare generics for super class
     * ({@code Some extends Base} where {@code class Base<T> }) and, in this case, parent class generics could
     * be resolved only by lower bound. Note that parent type analysis must be performed only when generics
     * for perent type are not known ahead of time (inlying resolution cases).
     *
     * @param type     type to analyze parent class for
     * @param generics known type generics
     * @return resolved parent class generics
     */
    private static LinkedHashMap<String, Type> analyzeParent(final Class type,
                                                             final Map<String, Type> generics) {
        LinkedHashMap<String, Type> res = null;
        final Class parent = type.getSuperclass();
        if (!type.isInterface() && parent != null && parent != Object.class
                && type.getGenericSuperclass() instanceof ParameterizedType) {
            res = resolveGenerics(type.getGenericSuperclass(), generics);
        } else if (parent != null && parent.getTypeParameters().length > 0) {
            // root class didn't declare generics
            res = resolveRawGenerics(parent);
        }
        return res == null ? EMPTY_MAP : res;
    }
}

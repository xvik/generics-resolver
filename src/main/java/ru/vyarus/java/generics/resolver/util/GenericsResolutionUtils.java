package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.util.walk.ComparatorTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.CompatibilityTypesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.TypesWalker;

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
@SuppressWarnings({"PMD.LooseCoupling", "PMD.GodClass"})
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
        try {
            analyzeType(generics, type, knownGenerics, ignoreClasses);
        } catch (Exception ex) {
            throw new IllegalStateException(String.format("Failed to analyze hierarchy for %s%s",
                    TypeToStringUtils.toStringClassWithGenerics(type, rootGenerics),
                    formatKnownGenerics(knownGenerics)), ex);
        }
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

                if (types.containsKey(interfaceType)) {
                    // class hierarchy may contain multiple implementations for the same interface
                    // in this case we can merge known generics, using most specific types
                    // (root type unifies interfaces, so we just collecting actual maximum known info
                    // from multiple sources)
                    try {
                        merge(generics, types.get(interfaceType));
                    } catch (Exception ex) {
                        throw new IllegalStateException(String.format(
                                "Interface %s appears multiple times in class hierarchy with "
                                        + "incompatible parametrization", interfaceType.getSimpleName()), ex);
                    }
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

    private static void merge(final LinkedHashMap<String, Type> main,
                              final LinkedHashMap<String, Type> additional) {
        for (Map.Entry<String, Type> entry : additional.entrySet()) {
            final String generic = entry.getKey();
            final Type value = entry.getValue();
            final Type currentValue = main.get(generic);

            if (isCompatible(value, currentValue)) {
                main.put(generic, getMoreSpecificType(value, currentValue));
            } else {
                // all variables already replaces, so no actual generics required
                throw new IllegalStateException(String.format(
                        "Incompatible values found for generic %s: %s and %s",
                        generic, TypeToStringUtils.toStringType(currentValue, EMPTY_MAP),
                        TypeToStringUtils.toStringType(value, EMPTY_MAP)));
            }
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

    private static String formatKnownGenerics(final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics) {
        if (knownGenerics.isEmpty()) {
            return "";
        }
        final StringBuilder known = new StringBuilder(50);
        known.append(" (with known generics: ");
        boolean first = true;
        for (Map.Entry<Class<?>, LinkedHashMap<String, Type>> entry : knownGenerics.entrySet()) {
            known.append(first ? "" : ", ")
                    .append(TypeToStringUtils
                            .toStringClassWithGenerics(entry.getKey(), entry.getValue()));
            first = false;
        }
        known.append(')');
        return known.toString();
    }
}

package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.error.GenericsResolutionException;
import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException;

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
     * @param rootGenerics  resolved root type generics (including owner type generics)
     * @param knownGenerics type generics known before analysis (some middle class generics are known) and
     *                      could contain possible outer generics (types for sure not included in resolving type
     *                      hierarchy)
     * @param ignoreClasses classes to ignore during analysis
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
            throw new GenericsResolutionException(type, rootGenerics, knownGenerics, ex);
        }
        return generics;
    }

    /**
     * Resolve declared generics for type (actually declared generics in context of some type).
     * If provided class is inner class - resolves outer class generics as upper bound
     * <p>
     * If type is not {@link ParameterizedType} and so does not contains actual generics info, resolve
     * generics from type declaration ({@link #resolveRawGenerics(Class)}),
     *
     * @param type     type to resolve generics for
     * @param generics generics of context class
     * @return resolved generics of parameterized type or empty map
     */
    public static LinkedHashMap<String, Type> resolveGenerics(final Type type,
                                                              final Map<String, Type> generics) {
        final LinkedHashMap<String, Type> res;
        if (type instanceof ParameterizedType) {
            res = new LinkedHashMap<String, Type>();
            final ParameterizedType actualType = (ParameterizedType) type;
            final Type[] genericTypes = actualType.getActualTypeArguments();
            final Class target = (Class) actualType.getRawType();
            final TypeVariable[] genericNames = target.getTypeParameters();

            // inner class can use outer class generics
            if (actualType.getOwnerType() != null) {
                fillOuterGenerics(target, res, null);
            }

            final int cnt = genericNames.length;
            for (int i = 0; i < cnt; i++) {
                final Type resolvedGenericType = GenericsUtils.resolveTypeVariables(genericTypes[i], generics);
                res.put(genericNames[i].getName(), resolvedGenericType);
            }
        } else {
            res = resolveRawGenerics(GenericsUtils.resolveClass(type, generics));
        }
        return res;
    }

    /**
     * Resolve type generics by declaration (as lower bound). Used for cases when actual generic definition is not
     * available (so actual generics are unknown). In most cases such generics resolved as Object
     * (for example, {@code Some<T>}).
     * <p>
     * If class is inner class, resolve outer class generics (which may be used in class)
     *
     * @param type class to analyze generics for
     * @return resolved generics or empty map if not generics used
     */
    public static LinkedHashMap<String, Type> resolveRawGenerics(final Class<?> type) {
        final TypeVariable[] declaredGenerics = type.getTypeParameters();
        final LinkedHashMap<String, Type> res = new LinkedHashMap<String, Type>();
        // inner class can use outer class generics
        fillOuterGenerics(type, res, null);
        for (TypeVariable variable : declaredGenerics) {
            res.put(variable.getName(), GenericsUtils.resolveTypeVariables(variable.getBounds()[0], res));
        }
        return res;
    }

    /**
     * Inner class could reference outer class generics and so this generics must be included into class context.
     * Outer class generics resolved as lower bound (by declaration). Class may declare the same generic name
     * and, in this case, outer generic will be ignored (as not visible).
     * <p>
     * During inlying context resolution we know outer context, and, if outer class is in outer context hierarchy,
     * we could assume that it's actual parent class and so we could use more specific generics. This will be true for
     * most sane cases (often inner class is used inside owner), but other cases are still possible (anyway,
     * the chance that inner class will appear in two different hierarchies of outer class is quite small).
     *
     * @param type          context type
     * @param generics      resolved type generics
     * @param knownGenerics map of known middle generics and possibly known outer context (outer context may contain
     *                      outer class generics declarations). May be null.
     */
    public static void fillOuterGenerics(final Class<?> type,
                                         final Map<String, Type> generics,
                                         final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics) {
        final Class<?> outer = TypeUtils.getOuter(type);
        if (outer == null) {
            // not inner class
            return;
        }
        final Map<String, Type> outerGenerics = knownGenerics != null && knownGenerics.containsKey(outer)
                ? new LinkedHashMap<String, Type>(knownGenerics.get(outer))
                : resolveRawGenerics(outer);
        // class may declare generics with the same name and they must not be overridden
        for (TypeVariable var : type.getTypeParameters()) {
            outerGenerics.remove(var.getName());
        }
        generics.putAll(outerGenerics);
    }

    /**
     * Analyze type hierarchy (all subclasses and interfaces).
     *
     * @param generics      resolved generics of already analyzed types
     * @param knownGenerics type generics known before analysis (some middle class generics are known) and
     *                      possible owner types (types not present in analyzed type hierarchy)
     * @param type          class to analyze
     * @param ignoreClasses classes to ignore during analysis
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
            fillOuterGenerics(next, generics.get(next), knownGenerics);
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
     * @param ignoreClasses classes to ignore during analysis
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
                    merge(interfaceType, generics, types.get(interfaceType));
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

    private static void merge(final Class<?> type,
                              final LinkedHashMap<String, Type> main,
                              final LinkedHashMap<String, Type> additional) {
        for (Map.Entry<String, Type> entry : additional.entrySet()) {
            final String generic = entry.getKey();
            final Type value = entry.getValue();
            final Type currentValue = main.get(generic);

            if (TypeUtils.isCompatible(value, currentValue)) {
                main.put(generic, TypeUtils.getMoreSpecificType(value, currentValue));
            } else {
                // all variables already replaces, so no actual generics required
                throw new IncompatibleTypesException(String.format(
                        "Interface %s appears multiple times in root class hierarchy with incompatible "
                                + "parametrization for generic %s: %%s and %%s",
                        TypeToStringUtils.toStringWithNamedGenerics(type), generic), currentValue, value);
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
}

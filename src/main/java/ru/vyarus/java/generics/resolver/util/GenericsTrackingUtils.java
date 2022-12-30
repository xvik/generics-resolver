package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.GenericsResolver;
import ru.vyarus.java.generics.resolver.context.GenericsContext;
import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;
import ru.vyarus.java.generics.resolver.error.GenericsTrackingException;
import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utilities to track root generic value from some known subtype generics.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
// LinkedHashMap used instead of usual map to avoid accidental simple map usage (order is important!)
@SuppressWarnings({"PMD.LooseCoupling", "checkstyle:IllegalIdentifierName"})
public final class GenericsTrackingUtils {

    private GenericsTrackingUtils() {
    }

    /**
     * Track root generics with known middle type generic. For example, {@code Some<P> extends Base<P>}
     * and we know generic of {@code Base<T>} then it is possible to track that P == T and so known.
     *
     * @param type          root type to track generics for
     * @param known         class or interface with known generics (in the middle of root type hierarchy)
     * @param knownGenerics generics of known type
     * @return root class generics (row types were impossible to track)
     * @throws IllegalStateException when resolved generic of known type contradict with known generic value
     *                               (type can't be casted to known type)
     * @see GenericsUtils#trackGenerics(Type, Type) shortcut for most common case
     */
    public static LinkedHashMap<String, Type> track(final Class<?> type,
                                                    final Class<?> known,
                                                    final LinkedHashMap<String, Type> knownGenerics) {
        if (type.getTypeParameters().length == 0 || knownGenerics.isEmpty()) {
            return EmptyGenericsMap.getInstance();
        }

        try {
            return trackGenerics(type, known, knownGenerics);
        } catch (Exception ex) {
            throw new GenericsTrackingException(type, known, knownGenerics, ex);
        }
    }


    /**
     * Base idea: resolving class hierarchy with root generics as variables and compare resolved known type generics
     * with actual generics (in the simplest case {@code Some<T> extends Base<T>} we will get
     * {@code TypeVariable(T) == known generic (of Base)}; other cases eventually leads to this one, e.g.
     * {@code Some<T> extends Base<List<T>>}).
     *
     * @param type          root type to track generics for
     * @param known         class or interface with known generics (in the middle of root type hierarchy)
     * @param knownGenerics generics of known type
     * @return root class generics (row types were impossible to track)
     * @throws IllegalStateException when resolved generic of known type contradict with known generic value
     *                               (type can't be casted to known type)
     */
    private static LinkedHashMap<String, Type> trackGenerics(final Class<?> type,
                                                             final Class<?> known,
                                                             final LinkedHashMap<String, Type> knownGenerics) {
        final Map<Class<?>, LinkedHashMap<String, Type>> generics = TypeVariableUtils.trackRootVariables(type);

        // trace back generics (what we can)
        final Map<String, Type> tracedRootGenerics = new HashMap<>();
        // required to check tracked type compatibility
        final Map<String, Type> rawRootGenerics = GenericsResolutionUtils.resolveRawGenerics(type);
        for (Map.Entry<String, Type> entry : generics.get(known).entrySet()) {
            final Type actualType = entry.getValue();
            final String genericName = entry.getKey();
            final Type knownGenericType = knownGenerics.get(genericName);

            trackType(tracedRootGenerics, rawRootGenerics,
                    genericName, actualType, knownGenericType, type, known, knownGenerics);
        }

        trackDependentVariables(type, tracedRootGenerics);

        // resolve all generics in correct resolution order
        final Map<String, Type> tmpTypes = new HashMap<>(tracedRootGenerics);
        for (TypeVariable gen : GenericsUtils.orderVariablesForResolution(type.getTypeParameters())) {
            final String name = gen.getName();
            final Type value = tracedRootGenerics.containsKey(name)
                    ? tracedRootGenerics.get(name)
                    // transform to wildcard to preserve possible multiple bounds declaration
                    // (it will be flatten to Object if single bound declared)
                    : GenericsUtils.resolveTypeVariables(gen.getBounds().length > 1
                    ? WildcardTypeImpl.upper(gen.getBounds()) : gen.getBounds()[0], tmpTypes);
            tmpTypes.put(name, value);
        }

        // finally apply correct generics order
        final LinkedHashMap<String, Type> res = new LinkedHashMap<>();
        for (TypeVariable gen : type.getTypeParameters()) {
            res.put(gen.getName(), tmpTypes.get(gen.getName()));
        }
        return res;
    }

    /**
     * The simplest case: {@code Root<T> extends Base<T>} where we need to compare {@code TypeVariable} with known
     * generic type and immediately get root generic value.
     * <p>
     * More complex cases examples:
     * <ul>
     * <li>{@code Root&lth;A> extends Base&lth;A[]>}</li>
     * <li>{@code Root&lth;A> extends Base&lth;List&lth;A>>}. If actual generic value is higher then declared type
     * {@code Base&lth;ArrayList&lth;String>>} then known generic hierarchy must be built (inlying context) to know
     * generics of lower type. In opposite situation, when generic type is lower then
     * known generic {@code Root&lth;A> extends Base&lth;ArrayList&lth;A>>} and {@code Base&lth;List&lth;String>},
     * actual generic is tracked (inception!)</li>
     * <li>Wildcards are used for type compatibility checks: {@code Root&lth;A extends Integer> extends Base&lth;A>}
     * and if {@code Base&lth;String>} then types are incompatible (Root can't be casted to {@code Base<&lth;String>}
     * </li>
     * <li>Dependant root generics {@code Root&lth;A, B extends A> extends Base&lth;A>}. Here A could be tracked and B
     * resolved to tracked A (as upper bound)</li>
     * <li>If known generic is wildcard {@code Base&lth;? extends Integer>}, then resolved root value would be
     * upper bound (Integer) for simplicity</li>
     * </ul>
     *
     * @param resolved        collection with all tracked root generics
     * @param rawRootGenerics raw resolution of root generics (low bounds), required to check types compatibility
     * @param genericName     introspected known type's generic name
     * @param actualGeneric   generic type, resolved from root class
     * @param knownGeneric    known generic type
     * @param root            root class (building hierarchy for)
     * @param known           known class (sub type of root)
     * @param knownGenerics   known class's generic values
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    private static void trackType(final Map<String, Type> resolved,
                                  final Map<String, Type> rawRootGenerics,
                                  final String genericName,
                                  final Type actualGeneric,
                                  final Type knownGeneric,
                                  final Class<?> root,
                                  final Class<?> known,
                                  final LinkedHashMap<String, Type> knownGenerics) {
        final Class<?> knownGenericType = GenericsUtils.resolveClass(knownGeneric, knownGenerics);

        if (actualGeneric instanceof ExplicitTypeVariable) {
            final ExplicitTypeVariable variable = (ExplicitTypeVariable) actualGeneric;
            // look what minimal type is acceptable according to root class declaration
            // Available if root class use wildcard ({@code <T extends Something>})
            checkTypesCompatibility(
                    // use wildcard to check possible multiple bounds definition
                    WildcardTypeImpl.upper(GenericsUtils.resolveTypeVariables(variable.getBounds(), rawRootGenerics)),
                    knownGenericType, genericName, root, known);
            resolved.put(variable.getName(), knownGeneric);
        } else if (actualGeneric instanceof ParameterizedType) {
            final Class<?> exactActualType = (Class) ((ParameterizedType) actualGeneric).getRawType();
            // look raw compatibility
            checkTypesCompatibility(exactActualType, knownGenericType, genericName, root, known);
            // if generic is not parameterized, but types compatible then simply types not declared properly -
            // nothing to do
            if (knownGeneric instanceof ParameterizedType) {
                // matching parametrization arguments, for example:
                // Root<A> extends Base<List<A>>, List<A> and known, for example, List<String>
                final Type[] actualArguments = ((ParameterizedType) actualGeneric).getActualTypeArguments();
                final Type[] knownArguments = alignParametrizationArguments(
                        exactActualType, knownGenericType,
                        (ParameterizedType) knownGeneric, knownGenerics);

                for (int i = 0; i < actualArguments.length; i++) {
                    // matching parametrization of actual type and known generic (already aligned to same type)
                    trackType(resolved, rawRootGenerics, genericName, actualArguments[i], knownArguments[i],
                            root, known, knownGenerics);
                }
            }
        } else if (actualGeneric instanceof GenericArrayType) {
            // compare base array types, for example: Root<A> extends Base<A[]>
            final Type actualComponentType = ((GenericArrayType) actualGeneric).getGenericComponentType();
            if (knownGeneric instanceof Class && ((Class) knownGeneric).isArray()) {
                trackType(resolved, rawRootGenerics, genericName, actualComponentType,
                        ((Class) knownGeneric).getComponentType(), root, known, knownGenerics);
            }
        } else if (actualGeneric instanceof Class) {
            // If generic was resolved to class then it's directly declared in the root class hierarchy
            // (Root extends Base<Something>).. tracking impossible
            // We only need to check that class not contradicts with known (declared) generic type.
            final Class exactActualType = (Class) actualGeneric;
            checkTypesCompatibility(exactActualType, knownGenericType, genericName, root, known);
        }
        // otherwise, in case of different type (possible?).. do nothing (give up)
    }

    private static Type[] alignParametrizationArguments(final Class<?> exactActualType,
                                                        final Class<?> knownGenericType,
                                                        final ParameterizedType knownGeneric,
                                                        final LinkedHashMap<String, Type> knownGenerics) {

        final Type[] knownArguments;

        // if base types are equal we can match types in parametrization
        if (exactActualType.equals(knownGenericType)) {
            knownArguments = knownGeneric.getActualTypeArguments();
        } else {
            // known generic type is a subclass of resolved root type.. inception!
            // trying to track generics
            if (knownGenericType.isAssignableFrom(exactActualType)) {
                // Actual type is higher then declared in generic: need to analyze this mismatch
                // (again not known root generics and known generics in sub type)
                final LinkedHashMap<String, Type> sub = track(exactActualType, knownGenericType,
                        GenericsResolutionUtils.resolveGenerics(knownGeneric, knownGenerics));
                knownArguments = sub.values().toArray(new Type[0]);
            } else {
                // actual class, resolved in root class hierarchy is a subtype of known generic type
                // building hierarchy for known generic value class and look generics of required subclass
                final GenericsContext ctx = GenericsResolver.resolve(knownGenericType);
                knownArguments = GenericInfoUtils.create(ctx, knownGeneric)
                        .getTypeGenerics(exactActualType).values().toArray(new Type[0]);
            }
        }
        return knownArguments;
    }

    /**
     * Check if known class generic resolved in current root class hierarchy is compatible with known generic value.
     * For example, if {@code Root<T extends Integer> extends Base<T>} and we resolve hierarchy for known
     * {@code Base<String>} then types are not compatible: for Root hierarchy we have {@code Target<Integer>} which
     * is not compatible with {@code String}.
     *
     * @param actualType  generic type in root class hierarchy
     * @param knownType   known generic type
     * @param genericName name of generic variable in known class
     * @param root        root class (hierarchy resolved for, can be casted to known)
     * @param known       class with known generics
     */
    private static void checkTypesCompatibility(final Type actualType,
                                                final Type knownType,
                                                final String genericName,
                                                final Class<?> root,
                                                final Class<?> known) {
        if (!TypeUtils.isCompatible(actualType, knownType)) {
            throw new IncompatibleTypesException(String.format(
                    "Known generic %s of %s is not compatible with %s hierarchy: %%s when required %%s",
                    genericName, TypeToStringUtils.toStringWithNamedGenerics(known),
                    TypeToStringUtils.toStringType(root)),
                    knownType, actualType);
        }
    }

    /**
     * Look if traced generics declarations contains other generics and so they could be resolved too.
     * For example, in {@code class Root<P, K extends List<P>> extends Base<K>} knowing K we can recover P.
     * <p>
     * All additionally resolved generic variables will be directly added to provided traces list.
     *
     * @param type               type generics were tracked for
     * @param tracedRootGenerics tracked generics
     */
    private static void trackDependentVariables(final Class<?> type,
                                                final Map<String, Type> tracedRootGenerics) {
        final TypeVariable<? extends Class<?>>[] typeParameters = type.getTypeParameters();
        if (tracedRootGenerics.isEmpty() || typeParameters.length == tracedRootGenerics.size()) {
            return;
        }
        final Map<String, TypeVariable> varsIndex = new HashMap<>();
        for (TypeVariable var : typeParameters) {
            varsIndex.put(var.getName(), var);
        }

        // find dependent generics - add to tracked - repeat with found only (cycle)
        final Map<String, Type> toCheck = new HashMap<>(tracedRootGenerics);
        final Map<String, Type> found = new HashMap<>();
        while (!toCheck.isEmpty()) {
            for (Map.Entry<String, Type> entry : toCheck.entrySet()) {
                found.putAll(
                        matchVariables(varsIndex.get(entry.getKey()), entry.getValue(), tracedRootGenerics));
            }

            // repeat cycle with newly found generics
            toCheck.clear();
            toCheck.putAll(found);
            found.clear();
        }
    }

    private static Map<String, Type> matchVariables(final TypeVariable declared,
                                                    final Type known,
                                                    final Map<String, Type> tracedRootGenerics) {
        final Map<String, Type> res = new HashMap<>();

        // lookup variable declaration for variables (e.g. T extends List<K>)
        for (Type decl : declared.getBounds()) {
            // the case: A extends B: when we know A we can't tell anything about B!
            if (decl instanceof TypeVariable) {
                continue;
            }
            final Map<String, Type> match = TypeVariableUtils.matchVariableNames(
                    TypeVariableUtils.preserveVariables(decl), known);

            // check if found match is more specific then already resolved
            for (Map.Entry<String, Type> matchEntry : match.entrySet()) {
                final String name = matchEntry.getKey();
                final Type value = matchEntry.getValue();
                if (tracedRootGenerics.containsKey(name)) {
                    final Type stored = tracedRootGenerics.get(name);
                    if (!TypeUtils.isMoreSpecific(value, stored)) {
                        // do nothing with type
                        continue;
                    }
                }
                tracedRootGenerics.put(name, value);
                res.put(name, value);
            }
        }
        return res;
    }
}

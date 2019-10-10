package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;
import ru.vyarus.java.generics.resolver.error.GenericsResolutionException;
import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException;
import ru.vyarus.java.generics.resolver.error.UnknownGenericException;
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;

import java.lang.reflect.*;
import java.util.*;

/**
 * Generics analysis utilities.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
// LinkedHashMap used instead of usual map to avoid accidental simple map usage (order is important!)
@SuppressWarnings({"PMD.LooseCoupling", "PMD.GodClass", "PMD.AvoidLiteralsInIfCondition"})
public final class GenericsResolutionUtils {

    private static final String GROOVY_OBJECT = "GroovyObject";

    private GenericsResolutionUtils() {
    }

    /**
     * Analyze class hierarchy and resolve actual generic values for all composing types. Root type generics
     * (if present) will be resolved as upper bound from declaration.
     *
     * @param type          type to resolve generics for
     * @param ignoreClasses classes to ignore (if required)
     * @return resolved generics for all types in class hierarchy
     * @see #resolve(Class, LinkedHashMap, Class[]) if you have known root generics
     * @see #resolve(Class, LinkedHashMap, Map, List) if you have known generics for middle types
     */
    public static Map<Class<?>, LinkedHashMap<String, Type>> resolve(final Class<?> type,
                                                                     final Class<?>... ignoreClasses) {
        return resolve(type, resolveRawGenerics(type), ignoreClasses);
    }

    /**
     * Resolve hierarchy of provided type. It makes sense only for {@link ParameterizedType} as only this type
     * could hold class generics. Parameter preserved to be {@link Type} for more universal usage
     * (to avoid instanceof checks), execution for simple {@link Class} is completely equal to
     * {@link #resolve(Class, Class[])} method call (original method preserved for api compatibility).
     * <p>
     * When provided type is not {@link ParameterizedType}, root class generics will be resolved
     * as upper bounds (the same as in {@link #resolve(Class, Class[])}).
     * <p>
     * IMPORTANT: If provided type contain variables ({@link TypeVariable}), they will be replaced by
     * {@link Object} (not upper bound!). Perform variables processing manually before calling this method
     * in order to get more precise results (e.g. with {@link TypeVariableUtils#resolveAllTypeVariables(Type)}).
     * <p>
     * You can also use {@link TypeLiteral} for dynamic type declarations (tracking cases):
     * {@code GenericsResolutionUtils.resolve(new TypeLiteral<MyClass<String, Boolean>(){}.getType())}.
     *
     * @param type          type to resolve generics for
     * @param ignoreClasses classes to ignore (if required)
     * @return resolved generics for all types in class hierarchy
     * @see #resolve(Class, LinkedHashMap, Class[]) if you have known root generics
     * @see #resolve(Class, LinkedHashMap, Map, List) if you have known generics for middle types
     */
    public static Map<Class<?>, LinkedHashMap<String, Type>> resolve(final Type type,
                                                                     final Class<?>... ignoreClasses) {
        return resolve(GenericsUtils.resolveClassIgnoringVariables(type),
                resolveGenerics(type, IgnoreGenericsMap.getInstance()), ignoreClasses);
    }

    /**
     * Analyze class hierarchy and resolve actual generic values for all composing types. Root type generics
     * are known.
     *
     * @param type          type to resolve generics for
     * @param rootGenerics  resolved root type generics (including owner type generics); must not be null!
     * @param ignoreClasses classes to ignore (if required
     * @return resolved generics for all types in class hierarchy
     */
    public static Map<Class<?>, LinkedHashMap<String, Type>> resolve(
            final Class<?> type,
            final LinkedHashMap<String, Type> rootGenerics,
            final Class<?>... ignoreClasses) {
        return resolve(type,
                rootGenerics,
                Collections.<Class<?>, LinkedHashMap<String, Type>>emptyMap(),
                Arrays.asList(ignoreClasses));
    }

    /**
     * Analyze class hierarchy and resolve actual generic values for all composing types.  Root type
     * generics must be specified directly.
     * <p>
     * If generics are known for some middle type, then they would be used "as is" instead of generics tracked
     * from root. Also, known generics will be used for lower hierarchy resolution.
     *
     * @param type          class to analyze
     * @param rootGenerics  resolved root type generics (including owner type generics); must not be null!
     * @param knownGenerics type generics known before analysis (some middle class generics are known) and
     *                      could contain possible outer generics (types for sure not included in resolving type
     *                      hierarchy); must not be null, but could be empty map
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
     * <p>
     * NOTE: for {@link WildcardType} returned map will contain only first upper bound type generics, resolved
     * by upper bound (because in many cases it would be impossible to build not contradicting generics map for
     * wildcard due to same generic names in different types).
     *
     * @param type     type to resolve generics for
     * @param generics generics of context class
     * @return resolved generics of parameterized type or empty map
     */
    public static LinkedHashMap<String, Type> resolveGenerics(final Type type,
                                                              final Map<String, Type> generics) {
        Type actual = type;
        if (type instanceof ParameterizedType) {
            // if parameterized type is not correct (contain only raw type without type arguments
            // (possible for instance types) then this call could unwrap it to pure class
            actual = GenericsUtils.resolveTypeVariables(type, generics);
        }
        final LinkedHashMap<String, Type> res;
        if (actual instanceof ParameterizedType) {
            final ParameterizedType actualType = (ParameterizedType) actual;
            final Type[] genericTypes = actualType.getActualTypeArguments();
            final Class target = (Class) actualType.getRawType();
            final TypeVariable[] genericNames = target.getTypeParameters();

            // inner class can use outer class generics
            res = fillOuterGenerics(actual, new LinkedHashMap<String, Type>(), null);

            final int cnt = genericNames.length;
            for (int i = 0; i < cnt; i++) {
                res.put(genericNames[i].getName(), genericTypes[i]);
            }
        } else {
            res = resolveRawGenerics(GenericsUtils.resolveClass(actual, generics));
        }
        return res;
    }

    /**
     * Resolve type generics by declaration (as upper bound). Used for cases when actual generic definition is not
     * available (so actual generics are unknown). In most cases such generics resolved as Object
     * (for example, {@code Some<T>}).
     * <p>
     * If class is inner class, resolve outer class generics (which may be used in class)
     *
     * @param type class to analyze generics for
     * @return resolved generics (including outer class generics) or empty map if not generics used
     * @see #resolveDirectRawGenerics(Class) to resolve without outer type
     */
    public static LinkedHashMap<String, Type> resolveRawGenerics(final Class<?> type) {
        // inner class can use outer class generics
        return fillOuterGenerics(type, resolveDirectRawGenerics(type), null);
    }

    /**
     * Resolve type generics by declaration (as upper bound). Used for cases when actual generic definition is not
     * available (so actual generics are unknown). In most cases such generics resolved as Object
     * (for example, {@code Some<T>}).
     * <p>
     * IMPORTANT: this method does not count possible outer class generics! Use
     * {@link #resolveRawGeneric(TypeVariable, LinkedHashMap)} as universal resolution method
     *
     * @param type class to analyze generics for
     * @return resolved generics or empty map if not generics used
     * @see #resolveRawGenerics(Class) to include outer type generics
     */
    public static LinkedHashMap<String, Type> resolveDirectRawGenerics(final Class<?> type) {
        return resolveRawGenericsChain(type.getTypeParameters(), null);
    }

    /**
     * Resolve method generics by declaration (as upper bound). Generics are resolved as upper bound (because it is
     * all available type information). For example, {@code public <T extends Serializable> do(T param)}
     * contains generic T which must be resolved as Serializable.
     *
     * @param method   method to analyze generics for
     * @param generics context class generics (which could be used in method generics declarations)
     * @return resolved generics or empty map if method does not contain generics
     */
    public static LinkedHashMap<String, Type> resolveDirectRawGenerics(final Method method,
                                                                       final Map<String, Type> generics) {
        return resolveRawGenericsChain(method.getTypeParameters(), generics);
    }

    /**
     * Extracts declared upper bound from generic declaration. For example, {@code Base<T>} will be resolved
     * as Object (default bound). {@code Base<T extends A & B>} resolved as "impossible" wildcard
     * {@code ? extends A & B} in order to preserve all known information.
     * <p>
     * Map of already resolved generics is required because declaration may rely on them. For example,
     * {@code Base<T extends Serializable, K extends T>} could be solved as {@code T = Serializable, K = Serializable}.
     *
     * @param variable generic declaration to analyze
     * @param generics already resolved generics (on the left of current)
     * @return either upper bound class or wildcard with multiple bounds
     */
    public static Type resolveRawGeneric(final TypeVariable variable,
                                         final LinkedHashMap<String, Type> generics) {
        final Type res;
        if (variable.getBounds().length > 1) {
            // case: T extends A & B -->  ? extends A & B
            final List<Type> types = new ArrayList<Type>();
            for (Type bound : variable.getBounds()) {
                // replace possible named generics with actual type (for cases like K extends T)
                final Type actual = GenericsUtils.resolveTypeVariables(bound, generics);
                if (actual instanceof WildcardType && ((WildcardType) actual).getUpperBounds().length > 0) {
                    // case: T extends A & B, K extends T & C --> K must be aggregated as ? extends A & B & C
                    // this case is impossible in java, but allowed in groovy
                    types.addAll(Arrays.asList(GenericsUtils
                            .resolveTypeVariables(((WildcardType) actual).getUpperBounds(), generics)));
                } else {
                    types.add(actual);
                }
            }
            // case: T extends Object & Something (may appear because of transitive generics resolution)
            // only one object could appear (because wildcard could only be
            // ? extends type (or generic) & exact interface (& exact interface))
            // (repackaged from type declaration)
            types.remove(Object.class);
            if (types.size() > 1) {
                // repackaging as impossible wildcard <? extends A & B> to store all known information
                res = WildcardTypeImpl.upper(types.toArray(new Type[0]));
            } else {
                // if one type remain - use it directly; if no types remain - use Object
                res = types.isEmpty() ? Object.class : types.get(0);
            }
        } else {
            // case: simple generic declaration <T> (implicitly extends Object)
            res = GenericsUtils.resolveTypeVariables(variable.getBounds()[0], generics);
        }
        return res;
    }

    /**
     * Inner class could reference outer class generics and so this generics must be included into class context.
     * <p>
     * Outer generics could be included into declaration like {@code Outer<String>.Inner field}. In this case
     * incoming type should be {@link ParameterizedType} with generified owner.
     * <p>
     * When provided type is simply a class (or anything resolvable to class), then outer class generics are resolved
     * as upper bound (by declaration). Class may declare the same generic name and, in this case, outer generic
     * will be ignored (as not visible).
     * <p>
     * During inlying context resolution, if outer generic is not declared in type we can try to guess it:
     * we know outer context, and, if outer class is in outer context hierarchy, we could assume that it's actual
     * parent class and so we could use more specific generics. This will be true for most sane cases (often inner
     * class is used inside owner), but other cases are still possible (anyway, the chance that inner class will
     * appear in two different hierarchies of outer class is quite small).
     * <p>
     * It is very important to use returned map instead of passed in map because, incoming empty map is always replaced
     * to avoid modifications of shared empty maps.
     *
     * @param type          context type
     * @param generics      resolved type generics
     * @param knownGenerics map of known middle generics and possibly known outer context (outer context may contain
     *                      outer class generics declarations). May be null.
     * @return actual generics map (incoming map may be default empty map and in this case it must be replaced)
     */
    public static LinkedHashMap<String, Type> fillOuterGenerics(
            final Type type,
            final LinkedHashMap<String, Type> generics,
            final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics) {
        final LinkedHashMap<String, Type> res;
        final Type outer = TypeUtils.getOuter(type);
        if (outer == null) {
            // not inner class
            res = generics;
        } else {
            final LinkedHashMap<String, Type> outerGenerics;
            if (outer instanceof ParameterizedType) {
                // outer generics declared in field definition (ignorance required because provided type
                // may contain unknown outer generics (Outer<B>.Inner field))
                outerGenerics = resolveGenerics(outer, new IgnoreGenericsMap(generics));
            } else {
                final Class<?> outerType = GenericsUtils.resolveClass(outer, generics);
                // either use known generics for outer class or resolve by upper bound
                outerGenerics = knownGenerics != null && knownGenerics.containsKey(outerType)
                        ? new LinkedHashMap<String, Type>(knownGenerics.get(outerType))
                        : resolveRawGenerics(outerType);
            }
            // class may declare generics with the same name and they must not be overridden
            for (TypeVariable var : GenericsUtils.resolveClass(type, generics).getTypeParameters()) {
                outerGenerics.remove(var.getName());
            }

            if (generics instanceof EmptyGenericsMap) {
                // if outer map is empty then it was assumed that empty map could be returned
                // (outerGenerics could be empty map too)
                res = outerGenerics;
            } else {
                generics.putAll(outerGenerics);
                res = generics;
            }
        }
        return res;
    }

    /**
     * Generic method for direct generics declaration analysis for class, method or constructor.
     * Generic declarations may depend on each other ({@code <T, K extends Collection<T>>}, cycle
     * ({@code <T extends Comparable<T>}) and depend on some other generics (hosting class generics).
     *
     * @param declaredGenerics declaration chain (chain is important because declarations may be dependent)
     * @param generics         known context generics or null
     * @return resolved generics or empty map if empty generic declarations provided
     */
    private static LinkedHashMap<String, Type> resolveRawGenericsChain(final TypeVariable[] declaredGenerics,
                                                                       final Map<String, Type> generics) {
        if (declaredGenerics.length == 0) {
            return EmptyGenericsMap.getInstance();
        }
        final LinkedHashMap<String, Type> contextGenerics = generics == null ? new LinkedHashMap<String, Type>()
                : new LinkedHashMap<String, Type>(generics);
        final LinkedHashMap<String, Type> res = new LinkedHashMap<String, Type>();
        final List<TypeVariable> failed = new ArrayList<TypeVariable>();
        // variables in declaration could be dependant and in any direction (e.g. <A extends List<B>, B>)
        // so assuming correct order at first, but if we face any error - order vars and resolve correctly
        // (it's better to avoid ordering by default as its required quite rarely)
        for (TypeVariable variable : declaredGenerics) {
            Type resolved = null;
            try {
                resolved = resolveRawGeneric(variable, contextGenerics);
            } catch (UnknownGenericException ex) {
                // direct cycle case Something<T extends Something<T>>
                // (failed generic is exactly resolving generic)
                if (ex.getGenericName().equals(variable.getName())
                        && ex.getGenericSource().equals(variable.getGenericDeclaration())) {
                    resolved = GenericsUtils.resolveClass(variable.getBounds()[0], contextGenerics);
                } else {
                    // variable will be stored with null to preserve order (without resolution) and resolved later
                    // (inversed declaration order case)
                    failed.add(variable);
                }
            }
            // storing null is OK! - just holding correct generic place
            res.put(variable.getName(), resolved);
            contextGenerics.put(variable.getName(), resolved);
        }
        if (!failed.isEmpty()) {
            for (TypeVariable variable : GenericsUtils.orderVariablesForResolution(failed)) {
                // replacing nulls in map
                final Type result = resolveRawGeneric(variable, contextGenerics);
                res.put(variable.getName(), result);
                contextGenerics.put(variable.getName(), result);
            }
        }
        return res;
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
            final LinkedHashMap<String, Type> nextGenerics = knownGenerics.containsKey(next)
                    ? knownGenerics.get(next)
                    : analyzeParent(supertype, generics.get(supertype));
            generics.put(next,
                    fillOuterGenerics(next, nextGenerics, knownGenerics));
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
                types.put(interfaceType, EmptyGenericsMap.getInstance());
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
     * be resolved only by upper bound. Note that parent type analysis must be performed only when generics
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
        return res == null ? EmptyGenericsMap.getInstance() : res;
    }
}

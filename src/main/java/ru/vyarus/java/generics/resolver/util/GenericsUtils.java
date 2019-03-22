package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.GenericDeclarationScope;
import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;
import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;
import ru.vyarus.java.generics.resolver.error.UnknownGenericException;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;

import java.lang.reflect.*;
import java.util.*;

/**
 * Helper utilities to correctly resolve generified types of super interfaces.
 *
 * @author Vyacheslav Rusakov
 * @since 17.10.2014
 */
@SuppressWarnings({"PMD.GodClass", "PMD.TooManyMethods"})
public final class GenericsUtils {

    private static final Type[] NO_TYPES = new Type[0];

    private GenericsUtils() {
    }

    /**
     * Called to properly resolve return type of root finder or inherited finder method.
     * Supposed to return enough type info to detect return type (collection, array or plain object).
     * <p>
     * Note: may return primitive because it might be important to differentiate actual value.
     * Use {@link TypeUtils#wrapPrimitive(Class)} to box possible primitive, if required.
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
     * If type is a variable, looks actual variable type, if it contains generics.
     * For {@link ParameterizedType} return actual type parameters, for simple class returns raw class generics.
     * <p>
     * Note: returned generics may contain variables inside!
     *
     * @param type     type to get generics of
     * @param generics context generics map
     * @return type generics array or empty array
     */
    public static Type[] getGenerics(final Type type, final Map<String, Type> generics) {
        Type[] res = NO_TYPES;
        Type analyzingType = type;
        if (type instanceof TypeVariable) {
            // if type is pure generic recovering parametrization
            analyzingType = declaredGeneric((TypeVariable) type, generics);
        }
        if ((analyzingType instanceof ParameterizedType)
                && ((ParameterizedType) analyzingType).getActualTypeArguments().length > 0) {
            res = ((ParameterizedType) analyzingType).getActualTypeArguments();
        } else if (type instanceof Class) {
            // if type is class return raw declaration
            final Class<?> actual = (Class<?>) analyzingType;
            if (actual.getTypeParameters().length > 0) {
                res = GenericsResolutionUtils.resolveDirectRawGenerics(actual)
                        .values().toArray(new Type[0]);
            }
        }
        return res;
    }

    /**
     * Called to properly resolve generified type (e.g. generified method return).
     * For example, when calling for {@code List<T>} it will return type of {@code T}.
     * <p>
     * If called on class (e.g. List) then return raw generic definition (upper bounds).
     *
     * @param type     type to analyze
     * @param generics root class generics mapping
     * @return resolved generic classes or empty list if type does not support generics
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     */
    public static List<Class<?>> resolveGenericsOf(final Type type, final Map<String, Type> generics) {
        final Type[] typeGenerics = getGenerics(type, generics);
        if (typeGenerics.length == 0) {
            return Collections.emptyList();
        }
        final List<Class<?>> res = new ArrayList<Class<?>>();
        for (Type gen : typeGenerics) {
            res.add(resolveClass(gen, generics));
        }
        return res;
    }

    /**
     * Shortcut for {@link #resolveClass(Type, Map)} (called with {@link IgnoreGenericsMap}). Could be used
     * when class must be resolved ignoring possible variables.
     * <p>
     * Note: {@code Object} will be used instead of variable even if it has upper bound declared (e.g.
     * {@code T extends Serializable}).
     *
     * @param type type to resolve
     * @return resolved class
     */
    public static Class<?> resolveClassIgnoringVariables(final Type type) {
        return resolveClass(type, IgnoreGenericsMap.getInstance());
    }

    /**
     * Shortcut for {@link #resolveClass(Type, Map)} (called with {@link EmptyGenericsMap}). Could be used
     * when provided type does not contain variables. If provided type contain variables, error will be thrown.
     *
     * @param type type to resolve
     * @return resolved class
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see #resolveClassIgnoringVariables(Type) to resolve type ignoring variables
     */
    public static Class<?> resolveClass(final Type type) {
        return resolveClass(type, EmptyGenericsMap.getInstance());
    }

    /**
     * Resolves top class for provided type (for example, for generified classes like {@code List<T>} it
     * returns base type List).
     * <p>
     * Note: may return primitive because it might be important to differentiate actual value.
     * Use {@link TypeUtils#wrapPrimitive(Class)} to box possible primitive, if required.
     *
     * @param type     type to resolve
     * @param generics root class generics mapping
     * @return resolved class
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see #resolveClass(Type) shortcut for types without variables
     * @see #resolveClassIgnoringVariables(Type) shortcut to resolve class ignoring passible variables
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
            res = ArrayTypeUtils.toArrayClass(
                    resolveClass(((GenericArrayType) type).getGenericComponentType(), generics));
        }
        return res;
    }

    /**
     * Resolve classes of provided types.
     * <p>
     * Note: may return primitives because it might be important to differentiate actual value.
     * Use {@link TypeUtils#wrapPrimitive(Class)} to box possible primitive, if required.
     *
     * @param types    types to resolve
     * @param generics type generics
     * @return list of resolved types classes
     */
    public static List<Class<?>> resolveClasses(final Type[] types, final Map<String, Type> generics) {
        final List<Class<?>> params = new ArrayList<Class<?>>();
        for (Type type : types) {
            params.add(resolveClass(type, generics));
        }
        return params;
    }

    /**
     * In most cases {@link #resolveClass(Type, Map)} could be used instead (for simplicity). This method will
     * only return different result for wildcards inside resolved types (where generics are replaced
     * {@link #resolveTypeVariables(Type, Map)}). Also, in contrast to {@link #resolveClass(Type, Map)},
     * method will replace primitive types with wrappers (int -&gt; Integer etc.) because this method is used mostly for
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
     * Special handling for {@link ExplicitTypeVariable} - this kind of type variable is not resolved and not
     * throw exception as unknown generic (thought as resolved type). This may be used for cases when type
     * variable must be preserved (like generics tracking or custom to string). In order to replace such
     * variables use {@link TypeVariableUtils#resolveAllTypeVariables(Type, Map)}.
     * <p>
     * Note that upper bounded wildcards are flattened to simple type ({@code ? extends Somthing -> Something} as
     * upper bounded wildcards are not useful at runtime. The only exception is wildcard with multiple bounds
     * (repackaged declaration {@code T extends A&B}). Wildcards with Object as bound are also flattened
     * ({@code List<? extends Object> --> List<Object>}, {@code List<?> --> List<Object>},
     * {@code List<? super Object> --> List<Object>}.
     *
     * @param type     type to resolve
     * @param generics root class generics mapping
     * @return resolved type
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see TypeVariableUtils#resolveAllTypeVariables(Type, Map)
     * @see #findVariables(Type)
     */
    public static Type resolveTypeVariables(final Type type, final Map<String, Type> generics) {
        return resolveTypeVariables(type, generics, false);
    }

    /**
     * Shortcut for {@link #resolveTypeVariables(Type, Map)} to process multiple types at once.
     *
     * @param types    types to replace named generics in
     * @param generics known generics
     * @return types without named generics
     * @see TypeVariableUtils#resolveAllTypeVariables(Type[], Map)
     */
    public static Type[] resolveTypeVariables(final Type[] types, final Map<String, Type> generics) {
        return resolveTypeVariables(types, generics, false);
    }

    // for TypeVariableUtils access
    @SuppressWarnings("PMD.AvoidProtectedMethodInFinalClassNotExtending")
    protected static Type resolveTypeVariables(final Type type,
                                               final Map<String, Type> generics,
                                               final boolean countPreservedVariables) {
        Type resolvedGenericType = null;
        if (type instanceof TypeVariable) {
            // simple named generics resolved to target types
            resolvedGenericType = declaredGeneric((TypeVariable) type, generics);
        } else if (type instanceof ExplicitTypeVariable) {
            // special type used to preserve named generic (and differentiate from type variable)
            resolvedGenericType = declaredGeneric((ExplicitTypeVariable) type, generics, countPreservedVariables);
        } else if (type instanceof Class) {
            resolvedGenericType = type;
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parametrizedType = (ParameterizedType) type;
            resolvedGenericType = new ParameterizedTypeImpl(parametrizedType.getRawType(),
                    resolveTypeVariables(parametrizedType.getActualTypeArguments(), generics, countPreservedVariables),
                    parametrizedType.getOwnerType());
        } else if (type instanceof GenericArrayType) {
            final GenericArrayType arrayType = (GenericArrayType) type;
            resolvedGenericType = new GenericArrayTypeImpl(resolveTypeVariables(
                    arrayType.getGenericComponentType(), generics, countPreservedVariables));
        } else if (type instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) type;
            if (wildcard.getLowerBounds().length > 0) {
                // only one lower bound could be (? super A)
                final Type lowerBound = resolveTypeVariables(
                        wildcard.getLowerBounds(), generics, countPreservedVariables)[0];
                // flatten <? super Object> to Object
                resolvedGenericType = lowerBound == Object.class
                        ? Object.class : WildcardTypeImpl.lower(lowerBound);
            } else {
                // could be multiple upper bounds because of named generic bounds repackage (T extends A & B)
                final Type[] upperBounds = resolveTypeVariables(
                        wildcard.getUpperBounds(), generics, countPreservedVariables);
                // flatten <? extends Object> (<?>) to Object and <? extends Something> to Something
                resolvedGenericType = upperBounds.length == 1 ? upperBounds[0] : WildcardTypeImpl.upper(upperBounds);
            }
        }
        return resolvedGenericType;
    }

    // for TypeVariableUtils access
    @SuppressWarnings("PMD.AvoidProtectedMethodInFinalClassNotExtending")
    protected static Type[] resolveTypeVariables(final Type[] types,
                                                 final Map<String, Type> generics,
                                                 final boolean countPreservedVariables) {
        if (types.length == 0) {
            return NO_TYPES;
        }
        final Type[] resolved = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = resolveTypeVariables(types[i], generics, countPreservedVariables);
        }
        return resolved;
    }

    /**
     * It is important to keep possible outer class generics, because they may be used in type declarations.
     * NOTE: It may be not all generics of owner type, but only visible owner generics.
     * <pre>{@code class Outer<T, K> {
     *      // outer generic T hidden
     *      class Inner<T> {}
     * }}</pre>
     * In order to recover possibly missed outer generics use {@code extractTypeGenerics(type, resultedMap)}
     * (may be required for proper owner type to string printing with all generics).
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
     * Return map itself for not inner class (or if no extra generics present in map).
     * <p>
     * In case when type's generic is not mentioned in map - it will be resolved from variable declaration.
     *
     * @param type     type
     * @param generics all type's context generics (self + outer class)
     * @return generics, declared on type ({@code A<T, K> -> T, K}) or empty map if no generics declared on type
     */
    public static Map<String, Type> extractTypeGenerics(final Class<?> type,
                                                        final Map<String, Type> generics) {
        // assuming generics map always contains correct generics and may include only outer
        // so if we call it with outer type and outer only generics it will correctly detect it
        final boolean enoughGenerics = type.getTypeParameters().length == generics.size();
        if (enoughGenerics) {
            return generics;
        }
        final LinkedHashMap<String, Type> res = new LinkedHashMap<String, Type>();
        // owner generics are all generics not mentioned in signature
        for (TypeVariable var : type.getTypeParameters()) {
            final String name = var.getName();
            if (generics.containsKey(name)) {
                res.put(name, generics.get(name));
            } else {
                // case: generic not provided in map may appear with outer class generics, which
                // may incompletely present in type's generic map (class may use generics with the same name)
                res.put(name, resolveClass(var.getBounds()[0], res));
            }
        }
        return res;
    }

    /**
     * @param variable generic variable
     * @return declaration class or null if not supported declaration source (should be impossible)
     */
    public static Class<?> getDeclarationClass(final TypeVariable variable) {
        return getDeclarationClass(variable.getGenericDeclaration());
    }

    /**
     * @param source generic declaration source (could be null)
     * @return declaration class or null if not supported declaration source (should be impossible)
     */
    public static Class<?> getDeclarationClass(final GenericDeclaration source) {
        Class<?> res = null;
        if (source != null) {
            if (source instanceof Class) {
                res = (Class<?>) source;
            } else if (source instanceof Method) {
                res = ((Method) source).getDeclaringClass();
            } else if (source instanceof Constructor) {
                res = ((Constructor) source).getDeclaringClass();
            }
        }
        return res;
    }

    /**
     * Converts type's known generics collection into generics map, suitable for usage with the api.
     * <p>
     * Note that if provided class is inner class then outer class generics will be added to the map to avoid
     * unknown generics while using api with this map
     * (see {@link GenericsResolutionUtils#fillOuterGenerics(Type, LinkedHashMap, Map)}).
     *
     * @param type     type to build generics map for
     * @param generics known generics (assumed in correct order)
     * @return map of type generics
     * @throws IllegalArgumentException if type's generics count don't match provided list
     */
    // LinkedHashMap indicates stored order, important for context
    @SuppressWarnings("PMD.LooseCoupling")
    public static LinkedHashMap<String, Type> createGenericsMap(final Class<?> type,
                                                                final List<? extends Type> generics) {
        final TypeVariable<? extends Class<?>>[] params = type.getTypeParameters();
        if (params.length != generics.size()) {
            throw new IllegalArgumentException(String.format(
                    "Can't build generics map for %s with %s because of incorrect generics count",
                    type.getSimpleName(), Arrays.toString(generics.toArray())));
        }
        final LinkedHashMap<String, Type> res = new LinkedHashMap<String, Type>();
        int i = 0;
        for (TypeVariable var : params) {
            res.put(var.getName(), generics.get(i++));
        }
        // add possible outer class generics to avoid unknown generics
        return GenericsResolutionUtils.fillOuterGenerics(type, res, null);
    }

    /**
     * Generics visibility (from inside context class):
     * <ul>
     * <li>Generics declared on class</li>
     * <li>Generics declared on outer class (if current is inner)</li>
     * <li>Constructor generics (if inside constructor)</li>
     * <li>Method generics (if inside method)</li>
     * </ul>.
     *
     * @param type          type to check
     * @param context       current context class
     * @param contextScope  current context scope (class, method, constructor)
     * @param contextSource context source object (required for method and constructor scopes)
     * @return first variable, containing generic not visible from current class or null if no violations
     */
    public static TypeVariable findIncompatibleVariable(final Type type,
                                                        final Class<?> context,
                                                        final GenericDeclarationScope contextScope,
                                                        final GenericDeclaration contextSource) {
        TypeVariable res = null;
        for (TypeVariable var : findVariables(type)) {
            final Class<?> target = getDeclarationClass(var);
            // declaration class must be context or it's outer class (even if outer = null equals will be correct)
            if (!target.equals(context) && !target.equals(TypeUtils.getOuter(context))) {
                res = var;
                break;
            }
            // e.g. correct class, but method generic when current context represents class
            if (!contextScope.isCompatible(GenericDeclarationScope.from(var.getGenericDeclaration()))
                    // e.g. method scope could match but actual methods differ
                    || contextSource != var.getGenericDeclaration()) {
                res = var;
                break;
            }
        }
        return res;
    }

    /**
     * Order variables for consequent variables resolution (to support reverse order declaration cases).
     * E.g. {@code T extends List<K>, K, P extends Collection<T>} must be resolved as 2, 1, 3 or
     * {@code T extends List<D>, D extends Collection<P>, P} must be resolved as 3, 2, 1 or
     * {@code T extends List<D>, P, D extends Collection<P>} must be resolved as 2, 3, 1
     * (otherwise resolution will fail due to unknown generic).
     * <p>
     * Note: incomplete set of variables could be provided: method order only provided vars, ignoring all
     * other variables (assuming they are known). This allows using this method inside error handler
     * (in order to process only not recognized vars).
     *
     * @param variables variables to order
     * @return variables ordered for correct types resolution
     */
    public static List<TypeVariable> orderVariablesForResolution(final List<TypeVariable> variables) {
        final List<TypeVariable> vars = new ArrayList<TypeVariable>(variables);
        final List<String> countableNames = new ArrayList<String>();
        for (TypeVariable var : variables) {
            countableNames.add(var.getName());
        }
        final List<String> known = new ArrayList<String>();
        final List<TypeVariable> res = new ArrayList<TypeVariable>();
        // cycle will definitely end because java compiler does not allow to specify generic cycles
        while (!vars.isEmpty()) {
            final Iterator<TypeVariable> it = vars.iterator();
            while (it.hasNext()) {
                final TypeVariable var = it.next();
                boolean reject = false;
                for (Type bound : var.getBounds()) {
                    // can't be empty as otherwise variables would not be here
                    final List<TypeVariable> unknowns = GenericsUtils.findVariables(bound);
                    for (TypeVariable unknown : unknowns) {
                        if (countableNames.contains(unknown.getName()) && !known.contains(unknown.getName())) {
                            reject = true;
                            break;
                        }
                    }
                }
                if (!reject) {
                    res.add(var);
                    known.add(var.getName());
                    it.remove();
                }
            }
        }

        return res;
    }

    /**
     * Shortcut for {@link #orderVariablesForResolution(List)} method to use for
     * {@link Class#getTypeParameters()} generics.
     *
     * @param variables variables to order
     * @return variables ordered for correct types resolution
     */
    public static List<TypeVariable> orderVariablesForResolution(final TypeVariable... variables) {
        return orderVariablesForResolution(Arrays.asList(variables));
    }

    /**
     * Searches for generic variable declarations in type. May be used for scope checks.
     * For example, in {@code List<T>} it will find "T", in {@code Some<Long, T, List<K>} "T" and "K".
     * <p>
     * Also detects preserved variables {@link ExplicitTypeVariable} (used for tracking).
     *
     * @param type type to analyze.
     * @return list of generic variables inside type or empty list
     */
    public static List<TypeVariable> findVariables(final Type type) {
        if (type instanceof Class) {
            return Collections.emptyList();
        }
        final List<TypeVariable> res = new ArrayList<TypeVariable>();
        findVariables(type, res);
        return res;
    }

    private static void findVariables(final Type type, final List<TypeVariable> found) {
        // note ExplicitTypeVariable is not checked as it's considered as known type
        if (type instanceof TypeVariable) {
            recordVariable((TypeVariable) type, found);
        } else if (type instanceof ExplicitTypeVariable) {
            recordVariable(((ExplicitTypeVariable) type).getDeclarationSource(), found);
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parametrizedType = (ParameterizedType) type;
            if (parametrizedType.getOwnerType() != null) {
                findVariables(parametrizedType.getOwnerType(), found);
            }
            for (Type par : parametrizedType.getActualTypeArguments()) {
                findVariables(par, found);
            }
        } else if (type instanceof GenericArrayType) {
            findVariables(((GenericArrayType) type).getGenericComponentType(), found);
        } else if (type instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) type;
            if (wildcard.getLowerBounds().length > 0) {
                // ? super
                findVariables(wildcard.getLowerBounds()[0], found);
            } else {
                // ? extends
                // in java only one bound could be defined, but here could actually be repackaged TypeVariable
                for (Type par : wildcard.getUpperBounds()) {
                    findVariables(par, found);
                }
            }
        }
    }

    // variables could also contain variables, e.g. <T, K extends List<T>>
    private static void recordVariable(final TypeVariable var, final List<TypeVariable> found) {
        // prevent cycles
        if (!found.contains(var)) {
            found.add(var);
            for (Type type : var.getBounds()) {
                findVariables(type, found);
            }
        }
    }

    private static Type declaredGeneric(final TypeVariable generic, final Map<String, Type> declarations) {
        final String name = generic.getName();
        final Type result = declarations.get(name);
        if (result == null) {
            throw new UnknownGenericException(name, generic.getGenericDeclaration());
        }
        return result;
    }

    private static Type declaredGeneric(final ExplicitTypeVariable generic,
                                        final Map<String, Type> declarations,
                                        final boolean resolve) {
        return resolve ? declaredGeneric(generic.getDeclarationSource(), declarations) : generic;
    }
}

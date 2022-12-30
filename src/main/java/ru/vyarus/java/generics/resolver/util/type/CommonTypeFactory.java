package ru.vyarus.java.generics.resolver.util.type;

import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;
import ru.vyarus.java.generics.resolver.util.ArrayTypeUtils;
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.*;

/**
 * Calculates common (base) type for provided types (maximum type to which both types could be downcasted).
 * Assumed to be used through {@link ru.vyarus.java.generics.resolver.util.TypeUtils#getCommonType(Type, Type)}.
 * Direct usage makes sense only when less precise type required (e.g. for {@link Integer} and {@link Double}
 * reduced type is {@link Number} and full type is {@code ? extends Number & Comparable<Number>}). Full type
 * remains all type information whereas reduced type is simpler in usage (for cases when maximum accuracy is not
 * required).
 *
 * @author Vyacheslav Rusakov
 * @since 22.03.2019
 */
@SuppressWarnings({"PMD.GodClass", "checkstyle:IllegalIdentifierName"})
public final class CommonTypeFactory {

    // specificity comparator (more specific types first)
    private static final Comparator<Type> TYPE_COMPARATOR = Collections.reverseOrder(new TypesComparator());

    private CommonTypeFactory() {
    }

    /**
     * Builds median type for provided types. {@link TypeUtils#getCommonType(Type, Type)} must be used in most
     * cases instead this direct call. Direct call is suitable only if less precise type is required (base class only).
     * <p>
     * Actually, interface restriction is used even in full types resolution because of possible loops.
     * For example, common type for {@link Integer} and {@link Double} is
     * {@code ? extends Number and Comparable<Number>}, but if we will use maximum precision for interface resolution
     * we will get loop because {@code Comparable} generic resolution is again require common type for {@link Integer}
     * and {@link Double}. To solve this, interface generics are resolved only till exact type.
     * <p>
     * Note that if we apply restricted resolution also for main types it will reduce accuracy (e.g.
     * just {@link Number} in example above), which may greatly reduce accuracy in multiple types comparision
     * (e.g. {@code Integer and Double and Comparable} (sequential match): when interfaces included result is
     * {@code Comparable} and without interfaces it's {@code Object}).
     * <p>
     * When resolution lead to multiple types (most likely, class + interfaces) they will be ordered as:
     * class, interface from non java package, interface with generics, by class name (without package).
     *
     * @param one                     first type
     * @param two                     second type
     * @param alwaysIncludeInterfaces true to resolve not only base class but also all common interfaces, false
     *                                to look common class only and lookup interfaces inly when no base class found
     * @return maximum class assignable to both types or {@code Object} if classes are incompatible
     */
    public static Type build(final Type one,
                             final Type two,
                             final boolean alwaysIncludeInterfaces) {
        // get rid of possible variables
        final Type first = cleanupTypeForComparison(one);
        final Type second = cleanupTypeForComparison(two);

        final Type res = buildImpl(first, second, alwaysIncludeInterfaces, new PathsCache());
        // during resolution wildcard types may be used as temporal placeholders (to prevent cycles)
        // and after resolution there might be wildcards with only one upper bound
        // so we need to repackage type to get rid of such wildcards
        return GenericsUtils.resolveTypeVariables(res, EmptyGenericsMap.getInstance());
    }

    private static Type buildImpl(final Type first,
                                  final Type second,
                                  final boolean alwaysIncludeInterfaces,
                                  final PathsCache cache) {
        final Type cached = cache.get(first, second);
        if (cached != null) {
            // prevent cycle with a special wrapper type (cleared after complete resolution)
            return cached;
        }
        cache.init(first, second);
        final Type res;

        if (TypeUtils.isCompatible(first, second)) {
            // the simplest case - types are compatible in any direction (or simply equal)
            res = TypeUtils.isMoreSpecific(first, second) ? second : first;

        } else if (ArrayTypeUtils.isArray(first) || ArrayTypeUtils.isArray(second)) {
            // special case for arrays (because of primitive arrays)
            res = checkArraysCommodity(first, second, alwaysIncludeInterfaces, cache);

        } else {
            // another special case for wildcards due to multiple types to compare
            if (first instanceof WildcardType || second instanceof WildcardType) {
                // searching wildcard type commodity: compare each type from left with each type from right
                // and return wildcard containing all not Object types.
                res = resolveWildcardCommodity(first, second, alwaysIncludeInterfaces, cache);

            } else {
                // two ordinary not equal types
                res = resolveCommonType(first, second, alwaysIncludeInterfaces, cache);
            }
        }

        cache.resolve(first, second, res);
        return res;
    }

    /**
     * Important step because consequent logic should not fail due to unknown generic. Due to java autoboxing
     * we can analyze only wrapper classes in order to properly find base types (in real life, resulted types
     * would be assignable: e.g. {@code Number num = (int) someInt}.
     *
     * @param type type to prepare for comparison
     * @return type without variables (resolved by upper bound) and wrapper class if type is primitive
     */
    private static Type cleanupTypeForComparison(final Type type) {
        final Type res = GenericsUtils.resolveTypeVariables(type, IgnoreGenericsMap.getInstance());
        return res instanceof Class ? TypeUtils.wrapPrimitive((Class) res) : res;
    }

    /**
     * Arrays in java are not affected by autoboxing and so primitive arrays could match only with Object or
     * primitive array with exactly the same type (e.g. {@code int[]} and {@code Integer[]} are not the same and
     * can't be assigned).
     * <p>
     * For not primitive types commodity calculated from contained type.
     * <p>
     * Assumed that direct types equality is already checked before call (with {@code TypeUtils.isCompatible()}).
     *
     * @param first                   first type
     * @param second                  second type
     * @param alwaysIncludeInterfaces always search for common interfaces
     * @param cache                   resolution types cache
     * @return common type or null if non of types is array
     */
    private static Type checkArraysCommodity(final Type first,
                                             final Type second,
                                             final boolean alwaysIncludeInterfaces,
                                             final PathsCache cache) {
        // special case for arrays because of primitive arrays
        final boolean firstArray = ArrayTypeUtils.isArray(first);
        final boolean secondArray = ArrayTypeUtils.isArray(second);

        final Type res;

        if (firstArray && secondArray) {
            // base array types must be matched
            final Type firstArrayType = ArrayTypeUtils.getArrayComponentType(first);
            final Type secondArrayType = ArrayTypeUtils.getArrayComponentType(second);

            // special case: primitives arrays are equal only to themselves (only same type or Object)
            if ((firstArrayType instanceof Class && ((Class) firstArrayType).isPrimitive())
                    || (secondArrayType instanceof Class && ((Class) secondArrayType).isPrimitive())) {
                // equal arrays case is implicitly checked before (by isCompatible), so these are not equal
                res = Object.class;
            } else {
                res = ArrayTypeUtils.toArrayType(
                        buildImpl(firstArrayType, secondArrayType, alwaysIncludeInterfaces, cache));
            }
        } else {
            // array can't share anything with non array
            res = Object.class;
        }
        return res;
    }

    /**
     * Used when one of types is wildcard (contains multiple declaration types like {@code ? extends One & Two}).
     * <p>
     * All contained types from one type is compared for commodity with all contained types in another type.
     * Overall commodity type is a wildcard with all non {@code Object} matches (of course after duplicates exclusion).
     *
     * @param first                   first type
     * @param second                  second type
     * @param alwaysIncludeInterfaces always search for common interfaces
     * @param cache                   resolution types cache
     * @return common type or Object if no commodity detected
     */
    private static Type resolveWildcardCommodity(final Type first,
                                                 final Type second,
                                                 final boolean alwaysIncludeInterfaces,
                                                 final PathsCache cache) {
        final List<Type> firstTypes = collectContainedTypes(first);
        final List<Type> secondTypes = collectContainedTypes(second);

        final List<Type> res = new ArrayList<>();
        // compare each left type with each right and all non Object matches will compose target common type
        for (Type left : firstTypes) {
            for (Type right : secondTypes) {
                final Type type = buildImpl(left, right, alwaysIncludeInterfaces, cache);
                if (type != Object.class) {
                    if (type instanceof WildcardType) {
                        // unwrap wildcard because we actually need only all classes
                        mergeWildcardTypes(res, ((WildcardType) type).getUpperBounds());
                    } else {
                        mergeWildcardTypes(res, type);
                    }
                }
            }
        }
        if (res.isEmpty()) {
            return Object.class;
        }
        return res.size() == 1 ? res.get(0) : WildcardTypeImpl.upper(res.toArray(new Type[0]));
    }

    /**
     * @param type type to resolve declaration types
     * @return upper bound types for {@link WildcardType} and type itself for non wildcard types
     */
    private static List<Type> collectContainedTypes(final Type type) {
        return type instanceof WildcardType
                ? Arrays.asList(((WildcardType) type).getUpperBounds())
                : Collections.singletonList(type);
    }

    /**
     * Adds new types to wildcard if types are not already contained. If added type is more specific then
     * already contained type then it replace such type.
     *
     * @param types    current wildcard types
     * @param addition new types to append to wildcard
     */
    private static void mergeWildcardTypes(final List<Type> types, final Type... addition) {
        if (types.isEmpty()) {
            Collections.addAll(types, addition);
            return;
        }
        for (Type add : addition) {
            final Class<?> addClass = GenericsUtils.resolveClass(add);
            final Iterator<Type> it = types.iterator();
            boolean additionApproved = true;
            while (it.hasNext()) {
                final Type type = it.next();
                // it can't contain incompatible generics just because of sane sense (type system will not allow)
                final Class<?> typeClass = GenericsUtils.resolveClass(type);
                if (addClass.isAssignableFrom(typeClass)) {
                    if (TypeUtils.isMoreSpecific(add, type)) {
                        // replace
                        it.remove();
                    } else {
                        // more specific type already contained
                        additionApproved = false;
                    }
                    break;
                }
            }
            if (additionApproved) {
                types.add(add);
            }
        }
    }


    /**
     * Pure search for median type without edge cases checks (no arrays or wildcards and not equal types).
     * Complete hierarchies are resolved for both types in order to find minimal base class and all
     * common interfaces.
     *
     * @param first                   first type
     * @param second                  second type
     * @param alwaysIncludeInterfaces always search for common interfaces
     * @param cache                   resolution types cache
     * @return resolved common type or Object if no commodity found
     */
    private static Type resolveCommonType(final Type first,
                                          final Type second,
                                          final boolean alwaysIncludeInterfaces,
                                          final PathsCache cache) {
        // resolve complete hierarchies, preserving all generics
        // (even if types are ParameterizedType it will be counted)
        final Map<Class<?>, LinkedHashMap<String, Type>> firstContext = resolveHierarchy(first);
        final Map<Class<?>, LinkedHashMap<String, Type>> secondContext = resolveHierarchy(second);

        // all types in hierarchies
        final Set<Class<?>> firstComposingTypes = new HashSet<>(firstContext.keySet());
        final Set<Class<?>> secondComposingTypes = new HashSet<>(secondContext.keySet());

        Class<?> commonRoot = Object.class;
        final Set<Class<?>> commonContracts = new HashSet<>();

        for (Class<?> type : firstComposingTypes) {
            if (secondComposingTypes.contains(type)) {
                if (type.isInterface()) {
                    commonContracts.add(type);
                } else {
                    // but it might be less specific as hierarchies contain all classes till Object
                    if (commonRoot.isAssignableFrom(type)) {
                        commonRoot = type;
                    }
                }
            }
        }

        return buildResultType(
                commonRoot, commonContracts, firstContext, secondContext, alwaysIncludeInterfaces, cache);
    }

    /**
     * If type is {@link java.lang.reflect.ParameterizedType} then it's generics will be still counted as root
     * class generics and so all sub type generics will be properly resolved.
     *
     * @param type type to build hierarchy for
     * @return complete type hierarchy with resolved generics
     */
    private static Map<Class<?>, LinkedHashMap<String, Type>> resolveHierarchy(final Type type) {
        return GenericsResolutionUtils.resolve(
                GenericsUtils.resolveClass(type),
                GenericsResolutionUtils.resolveGenerics(type, EmptyGenericsMap.getInstance()));
    }

    /**
     * When first level commodity found, complete types must be build (including all generics) so the entire
     * process is restarted for generic types and using all this final type is built.
     * <p>
     * This is the only place where {@code alwaysIncludeInterfaces} actually required! When full type is enabled,
     * not only base class will be resolved, but also all common interfaces (to provide the most complete
     * type without accuracy lost). If base class is not detected then all detected interfaces are simply returned
     * as wildcard (of course if only 1 interface found, it's directly returned without wildcard wrapper).
     *
     * @param type                    common root class
     * @param contracts               common interfaces
     * @param firstContext            first type generics context
     * @param secondContext           second type generics context
     * @param alwaysIncludeInterfaces always search for common interfaces
     * @param cache                   resolution types cache
     * @return final median type for original types
     */
    private static Type buildResultType(final Class<?> type,
                                        final Set<Class<?>> contracts,
                                        final Map<Class<?>, LinkedHashMap<String, Type>> firstContext,
                                        final Map<Class<?>, LinkedHashMap<String, Type>> secondContext,
                                        final boolean alwaysIncludeInterfaces,
                                        final PathsCache cache) {

        removeDuplicateContracts(type, contracts);

        final List<Type> res = new ArrayList<>();
        if (type != Object.class) {
            res.add(buildCommonType(type, firstContext, secondContext, alwaysIncludeInterfaces, cache));
        }

        // resolve interfaces only for root type resolution or if root class cant be found
        if (alwaysIncludeInterfaces || res.isEmpty()) {
            for (Class<?> iface : contracts) {
                // simpler resolution for contracts (only class to prevent cycles)
                res.add(buildCommonType(iface, firstContext, secondContext, false, cache));
            }
            // sort found types by specificity:
            // class -> interface from non java package -> interface with generic -> sort by name
            // in order to always get predictable order and so always the same resolution
            Collections.sort(res, TYPE_COMPARATOR);
        }

        return res.isEmpty() ? Object.class
                : res.size() == 1 ? res.iterator().next()
                : WildcardTypeImpl.upper(res.toArray(new Type[0]));
    }

    /**
     * After common types resolution we have some base class and set of common interfaces.
     * These interfaces may contain duplicates or be already covered by base class.
     *
     * @param type      base class
     * @param contracts common interfaces (to cleanup)
     */
    private static void removeDuplicateContracts(final Class<?> type, final Set<Class<?>> contracts) {
        if (contracts.isEmpty()) {
            return;
        }

        // remove interfaces already included in common type (e.g. Number and Serializable)
        Iterator<Class<?>> it = contracts.iterator();
        while (it.hasNext()) {
            if (it.next().isAssignableFrom(type)) {
                it.remove();
            }
        }

        // remove duplicate interfaces (e.g. List already include Collection and Iterable)
        it = contracts.iterator();
        while (it.hasNext()) {
            final Class<?> current = it.next();
            for (Class<?> iface : contracts) {
                if (!current.equals(iface) && current.isAssignableFrom(iface)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    /**
     * Called when common class is found in order to properly resolve generics for the common class.
     * For example, suppose we detect that {@link Comparable} is common for {@link Integer} and {@link Double}
     * and now we need to know it's common generic (in context of both types) which is {@code Comparable<Number>}.
     * <p>
     * Infinite generics depth is supported (e.g. {@code List<Set<List<T>>>>}. Types commodity resolution is simply
     * restarted for lower levels (recursion).
     *
     * @param type                    common type (may be interface) for original types
     * @param firstContext            first type generics context
     * @param secondContext           second type generics context
     * @param alwaysIncludeInterfaces always search for common interfaces
     * @param cache                   resolution types cache
     * @return complete common type
     */
    private static Type buildCommonType(final Class<?> type,
                                        final Map<Class<?>, LinkedHashMap<String, Type>> firstContext,
                                        final Map<Class<?>, LinkedHashMap<String, Type>> secondContext,
                                        final boolean alwaysIncludeInterfaces,
                                        final PathsCache cache) {
        final TypeVariable<? extends Class<?>>[] typeParameters = type.getTypeParameters();
        if (typeParameters.length > 0) {
            final Type[] params = new Type[typeParameters.length];
            final Map<String, Type> firstGenerics = firstContext.get(type);
            final Map<String, Type> secondGenerics = secondContext.get(type);
            int i = 0;
            boolean notAllObject = false;
            for (TypeVariable var : typeParameters) {
                final Type sub1 = firstGenerics.get(var.getName());
                final Type sub2 = secondGenerics.get(var.getName());
                final Type paramType = buildParameterType(type, sub1, sub2, alwaysIncludeInterfaces, cache);
                if (paramType != Object.class) {
                    notAllObject = true;
                }
                params[i++] = paramType;
            }
            final Type outer = TypeUtils.getOuter(type);
            // simplify to avoid redundant (actually absent) parametrization
            return outer != null || notAllObject
                    ? new ParameterizedTypeImpl(type, params, outer)
                    : type;
        } else {
            return type;
        }
    }

    /**
     * Method used instead of direct {@link #buildImpl(Type, Type, boolean, PathsCache)} call while computing
     * generics. Method may return customized placeholder type, which will try to resolve not to the same type
     * as root type (avoid situation like {@code Comparable<Comparable>}).
     *
     * @param root                    root type (computing generics for)
     * @param first                   first type
     * @param second                  second type
     * @param alwaysIncludeInterfaces true to resolve not only base class but also all common interfaces, false
     *                                to look common class only and lookup interfaces inly when no base class found
     * @return maximum class assignable to both types or {@code Object} if classes are incompatible
     */
    private static Type buildParameterType(final Class<?> root,
                                           final Type first,
                                           final Type second,
                                           final boolean alwaysIncludeInterfaces,
                                           final PathsCache cache) {
        Type res = buildImpl(first, second, alwaysIncludeInterfaces, cache);
        if (res instanceof PlaceholderType) {
            res = ((PlaceholderType) res).forRoot(root);
        }
        return res;
    }

    /**
     * Internal types resolution cache used to prevent infinite cycles. For example,
     * {@code Integer extends Number implements Comparable<Integer>} and
     * {@code String implements Comparable<String>}: without cache it would be an infinite loop
     * of {@code String} and {@code Integer} resolutions due to {@code Comparable} interface.
     */
    private static class PathsCache {
        private final Map<TypesKey, PlaceholderType> cache = new HashMap<>();

        public PlaceholderType get(final Type one, final Type two) {
            return cache.get(key(one, two));
        }

        /**
         * Before types commodity resolution, putting empty placeholder into cache to prevent cycles.
         *
         * @param one first type
         * @param two second type
         */
        public void init(final Type one, final Type two) {
            final TypesKey key = key(one, two);
            if (cache.containsKey(key)) {
                // resolution logic error
                throw new IllegalStateException(String.format(
                        "Cache already contains placeholder for types %s and %s",
                        TypeToStringUtils.toStringType(one),
                        TypeToStringUtils.toStringType(two)));
            }
            cache.put(key, new PlaceholderType());
        }

        /**
         * Resolve placeholder (in all places it could be possibly used) - delayed typification.
         *
         * @param one   first type
         * @param two   second type
         * @param value common type
         */
        public void resolve(final Type one, final Type two, final Type value) {
            cache.get(key(one, two)).resolve(value);
        }

        private TypesKey key(final Type one, final Type two) {
            return new TypesKey(one, two);
        }
    }

    /**
     * Type pair unification object to use as map key.
     */
    private static final class TypesKey {
        private final Type one;
        private final Type two;

        private TypesKey(final Type one, final Type two) {
            this.one = one;
            this.two = two;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof TypesKey)) {
                return false;
            }

            final TypesKey typePair = (TypesKey) o;

            if (!one.equals(typePair.one)) {
                return false;
            }
            return two.equals(typePair.two);
        }

        @Override
        public int hashCode() {
            int result = one.hashCode();
            result = 31 * result + two.hashCode();
            return result;
        }
    }

    /**
     * Type used as placeholder to avoid cycles during resolution. As this will be used only during interfaces
     * resolution, the result is simplified (for example, when full type is class + interfaces, inside implemented
     * interfaces there is no need for such accuracy, so only first resolved type could be taken (enough precision)).
     * <p>
     * Where possible, sub type is created in order to avoid duplication (e.g. {@code Comparable<Comparable>}).
     * If type was resolved as wildcard with multiple types (class + interfaces) then different type would be selected.
     * <p>
     * This type is used only inside this factory and never leave it, because
     * {@link GenericsUtils#resolveTypeVariables(Type, Map)} always replace wildcards with one upper bound with
     * actual bound. And that's why no equals or hash code is implemented in type - no need.
     */
    private static class PlaceholderType implements WildcardType {
        private static final Type[] EMPTY = new Type[0];
        private List<PlaceholderType> placeholders;

        private final Class<?> root;
        private Type[] upperBound;

        // root placeholder
        PlaceholderType() {
            this(null);
        }

        // derived placeholder
        private PlaceholderType(final Class<?> root) {
            this.root = root;
        }

        /**
         * Called when types resolution finished in order to resolve placeholder with real type.
         *
         * @param bound real type
         */
        public void resolve(final Type bound) {
            if (upperBound != null) {
                throw new IllegalArgumentException("Placeholder already resolved");
            }

            // reduce accuracy because otherwise infinite cycles are possible
            // besides, placeholders may appear only on implemented interfaces and there exact type is not important
            Class<?> res = GenericsUtils.resolveClass(bound);
            if (root == null) {
                // notify derived placeholders
                if (placeholders != null) {
                    for (PlaceholderType placeholder : placeholders) {
                        placeholder.resolve(bound);
                    }
                }
            } else {
                // try to use different type if possible (to avoid Some<Some> cases)
                if (res.equals(root)
                        && bound instanceof WildcardType && (((WildcardType) bound).getUpperBounds()).length > 1) {
                    // use second type by specificity
                    res = GenericsUtils.resolveClass(((WildcardType) bound).getUpperBounds()[1]);
                }
            }
            this.upperBound = new Type[]{res};
        }

        @Override
        @SuppressWarnings("PMD.MethodReturnsInternalArray")
        public Type[] getUpperBounds() {
            return upperBound == null ? EMPTY : upperBound;
        }

        @Override
        public Type[] getLowerBounds() {
            return new Type[0];
        }

        /**
         * When resolving generic parameters, it's highly not desirable to get duplicate chain like
         * {@code Comparable<Comparable>}, so creating custom placeholder which will try to correct this issue
         * on resolution (resolution is derived from main placeholder resolution).
         *
         * @param root root class (resolving generics for)
         * @return new placeholder
         */
        public PlaceholderType forRoot(final Class<?> root) {
            final PlaceholderType res = new PlaceholderType(root);
            // delayed init because placeholders often will not be used at all
            if (placeholders == null) {
                placeholders = new ArrayList<>();
            }
            placeholders.add(res);
            return res;
        }
    }

    /**
     * Comparator used to order all resolved types.
     * Rules:
     * <ul>
     * <li>Class</li>
     * <li>Interface from non java package (assume user interface)</li>
     * <li>Interface with generics</li>
     * <li>Order by name</li>
     * </ul>
     * This is required in order to always receive predictable results (in case of placeholders, only the first type
     * is used and so it's important to choose the most specific type).
     */
    private static class TypesComparator implements Comparator<Type>, Serializable {

        private static final String JAVA_PKG = "java.";

        @Override
        @SuppressWarnings("checkstyle:ReturnCount")
        public int compare(final Type o1, final Type o2) {
            final Class o1cls = GenericsUtils.resolveClass(o1);
            final Class o2cls = GenericsUtils.resolveClass(o2);

            // class goes first
            final boolean o1Interface = o1cls.isInterface();
            final boolean o2Interface = o2cls.isInterface();
            if (o1Interface != o2Interface) {
                return o1Interface ? -1 : 1;
            }

            // non hava class goes first
            final boolean o1FromJava = o1cls.getPackage().getName().startsWith(JAVA_PKG);
            final boolean o2FromJava = o2cls.getPackage().getName().startsWith(JAVA_PKG);
            if (o1FromJava != o2FromJava) {
                return o1FromJava ? -1 : 1;
            }

            // type with generics assumed more specific
            final boolean o1Generics = o1cls.getTypeParameters().length > 0;
            final boolean o2Generics = o2cls.getTypeParameters().length > 0;
            if (o1Generics != o2Generics) {
                return o1Generics ? 1 : -1;
            }

            // and finally sort by name
            return o1cls.getSimpleName().compareTo(o2cls.getSimpleName());
        }
    }
}

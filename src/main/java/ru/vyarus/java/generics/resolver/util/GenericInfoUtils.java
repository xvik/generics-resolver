package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.GenericsContext;
import ru.vyarus.java.generics.resolver.context.GenericsInfo;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Generic info creation logic. There are three major cases:
 * <ul>
 * <li>Class hierarchy resolution (possible root generics unknown) - default case</li>
 * <li>Class hierarchy resolution from type declaration inside resolved class (possible root generics are known) -
 * for deep analysis</li>
 * <li>Class hierarchy resolution when generics of some sub type are known (root generics could be partially
 * restored by tracking definition from known middle) - very rare case when object instance being analyzed and so
 * actual instance data could be used to help analysis</li>
 * </ul>
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2014
 */
// LinkedHashMap used instead of usual map to avoid accidental simple map usage (order is important!)
@SuppressWarnings("PMD.LooseCoupling")
public final class GenericInfoUtils {

    private GenericInfoUtils() {
    }

    /**
     * Root class analysis. If root class has generics - they will be resolved as lower known bound.
     * <p>
     * The result must be cached.
     *
     * @param type          class to analyze
     * @param ignoreClasses exclude classes from hierarchy analysis
     * @return analyzed type generics info
     */
    public static GenericsInfo create(final Class<?> type, final Class<?>... ignoreClasses) {
        // root class may contain generics or it may be inner class
        final LinkedHashMap<String, Type> generics = GenericsResolutionUtils.resolveRawGenerics(type);
        return create(type, generics, null, ignoreClasses);
    }

    /**
     * Type analysis in context of analyzed type. For example, resolution of field type class in context of
     * analyzed class (so we can correctly resolve it's generics).In essence, the only difference with usual type
     * resolution is known root generics.
     * <p>
     * The result is not intended to be cached as it's context-sensitive.
     *
     * @param context       generics context of containing class
     * @param type          type to analyze (important: this must be generified type and not raw class in
     *                      order to properly resolve generics)
     * @param ignoreClasses classes to exclude from hierarchy analysis
     * @return analyzed type generics info
     */
    public static GenericsInfo create(
            final GenericsContext context, final Type type, final Class<?>... ignoreClasses) {
        // root generics are required only to properly solve type
        final Map<String, Type> rootGenerics = context.genericsMap();
        // first step: solve type to replace transitive generics with direct values
        final Type actual = GenericsUtils.resolveTypeVariables(type, rootGenerics);
        final Class<?> target = context.resolveClass(actual);

        final LinkedHashMap<String, Type> generics = GenericsResolutionUtils.resolveGenerics(actual, rootGenerics);
        GenericsResolutionUtils.fillOuterGenerics(target, generics, context.getGenericsInfo().getTypesMap());
        return create(target, generics,
                // store possible owner types from parent context
                usePossiblyOwnerGenerics(target, context.getGenericsInfo()), ignoreClasses);
    }

    /**
     * Type analysis in context of analyzed type with child class as target type. Case: we have interface
     * (or base type) with generic in class (as field or return type), but we need to analyze actual
     * instance type (from value). This method will analyze type from new root (where generics are unknown), but
     * will add known middle generics.
     * <p>
     * NOTE: some of the root generics could possibly be resolved if there are any traceable connectivity between
     * the root class and known middle generics. All possible (known) cases should be solved. For example,
     * {@code Root<K> extends Target<List<K>>} when we know {@code Target<Collection<String>>} then
     * K will be tracked as String.
     * <p>
     * In essence: root generics are partially resolved by tracking definition from known middle class.
     * Other root generics resolved as lower bound (the same as in usual type resolution case).
     * If middle type generic is not specified (and so resolved as Object) then known specific type used
     * (assuming root type would be used in place with known parametrization and so more specifi generic may be
     * counted).
     * <p>
     * The result is not intended to be cached as it's context-sensitive.
     *
     * @param context       generics context of containing class
     * @param type          type to analyze (important: this must be generified type and not raw class in
     *                      order to properly resolve generics)
     * @param asType        target child type (this class contain original type in hierarchy)
     * @param ignoreClasses classes to exclude from hierarchy analysis
     * @return analyzed type generics info
     */
    public static GenericsInfo create(final GenericsContext context,
                                      final Type type,
                                      final Class<?> asType,
                                      final Class<?>... ignoreClasses) {
        // root generics are required only to properly solve type
        final Map<String, Type> rootGenerics = context.genericsMap();
        // first step: solve type to replace transitive generics with direct values
        final Type actual = GenericsUtils.resolveTypeVariables(type, rootGenerics);
        final Class<?> middleType = context.resolveClass(actual);
        if (!middleType.isAssignableFrom(asType)) {
            throw new IllegalArgumentException(String.format("Requested type %s is not a subtype of %s",
                    asType.getSimpleName(), middleType.getSimpleName()));
        }

        // known middle type
        LinkedHashMap<String, Type> typeGenerics = GenericsResolutionUtils
                .resolveGenerics(actual, rootGenerics);
        final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics =
                new HashMap<Class<?>, LinkedHashMap<String, Type>>();
        knownGenerics.put(middleType, typeGenerics);
        // store other types for possible outer classes generics resolution
        knownGenerics.putAll(usePossiblyOwnerGenerics(asType, context.getGenericsInfo()));

        // root type
        typeGenerics = asType.getTypeParameters().length > 0
                // special case: root class also contains generics
                ? GenericsTrackingUtils.track(asType, middleType, typeGenerics)
                // root type may be inner type and so could use outer class generics
                : GenericsResolutionUtils.resolveRawGenerics(asType);

        GenericsResolutionUtils.fillOuterGenerics(asType, typeGenerics, context.getGenericsInfo().getTypesMap());
        return create(asType, typeGenerics, knownGenerics, ignoreClasses);
    }


    private static GenericsInfo create(
            final Class type,
            final LinkedHashMap<String, Type> rootGenerics,
            final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics,
            final Class<?>... ignoreClasses) {

        final Map<Class<?>, LinkedHashMap<String, Type>> generics = GenericsResolutionUtils.resolve(type,
                rootGenerics,
                knownGenerics == null ? Collections.<Class<?>, LinkedHashMap<String, Type>>emptyMap() : knownGenerics,
                Arrays.asList(ignoreClasses));
        return new GenericsInfo(type, generics, ignoreClasses);
    }

    /**
     * When building inlying context, target type may be inner class, and if root context contains owner type
     * then we can assume that it's known more specific generics may be used. This is not correct in general,
     * as inner class may be created inside different class, but in most cases inner classes are used within
     * outer class and the chance that different outer class hierarchies will interact are quite low.
     * <p>
     * Storing all types, not present in target class hieararchy (to avoid affecting actual generics resolution)
     *
     * @param type target (inlying) type
     * @param info root context generics info (possibly outer)
     * @return possible owner classes, not present in target type hierarchy
     */
    private static Map<Class<?>, LinkedHashMap<String, Type>> usePossiblyOwnerGenerics(
            final Class<?> type, final GenericsInfo info) {
        final Map<Class<?>, LinkedHashMap<String, Type>> res = new HashMap<Class<?>, LinkedHashMap<String, Type>>();
        // use only types, not included in target hierarchy
        for (Class<?> root : info.getComposingTypes()) {
            if (!root.isAssignableFrom(type)) {
                res.put(root, (LinkedHashMap<String, Type>) info.getTypeGenerics(root));
            }
        }
        return res;
    }
}

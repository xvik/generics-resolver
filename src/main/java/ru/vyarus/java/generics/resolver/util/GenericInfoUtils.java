package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.GenericsContext;
import ru.vyarus.java.generics.resolver.context.GenericsInfo;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Generic info creation logic.
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2014
 */
// LinkedHashMap used instead of usual map to avoid accidental simple map usage (order is important!)
@SuppressWarnings("PMD.LooseCoupling")
public final class GenericInfoUtils {

    private static final LinkedHashMap<String, Type> EMPTY_MAP = new LinkedHashMap<String, Type>(0);
    private static final String GROOVY_OBJECT = "GroovyObject";

    private GenericInfoUtils() {
    }

    /**
     * Root class analysis. The result must be cached.
     *
     * @param type          class to analyze
     * @param ignoreClasses exclude classes from hierarchy analysis
     * @return analyzed type generics info
     */
    public static GenericsInfo create(final Class<?> type, final Class<?>... ignoreClasses) {
        final LinkedHashMap<String, Type> generics = type.getTypeParameters().length > 0
                // special case: root class also contains generics
                ? resolveRawGenerics(type.getTypeParameters())
                : EMPTY_MAP;
        return create(type, generics, null, ignoreClasses);
    }

    /**
     * Type analysis in context of analyzed type. For example, resolution of field type class in context of
     * analyzed class (so we can correctly resolve it's generics). The result is not intended to be cached as it's
     * context-sensitive.
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
        final Type actual = GenericsUtils.resolveTypeGenerics(type, rootGenerics);
        final Class target = context.resolveClass(actual);
        final LinkedHashMap<String, Type> generics = actual instanceof ParameterizedType
                ? resolveGenerics((ParameterizedType) actual, rootGenerics) : EMPTY_MAP;
        return create(target, generics, null, ignoreClasses);
    }

    /**
     * Type analysis in context of analyzed type with child class as target type. Case: we have interface
     * (or base type) with generic in class (as field or return type), but we need to analyze actual
     * instance type (from value). This method will analyze type from new root (where generics is unknown), but
     * will add known middle generics. The result is not intended to be cached as it's
     * context-sensitive.
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
        final Type actual = GenericsUtils.resolveTypeGenerics(type, rootGenerics);
        final Class<?> middleType = context.resolveClass(actual);
        if (!middleType.isAssignableFrom(asType)) {
            throw new IllegalArgumentException(String.format("Requested type %s is not a subtype of %s",
                    asType.getSimpleName(), middleType.getSimpleName()));
        }

        // known middle type
        LinkedHashMap<String, Type> typeGenerics = actual instanceof ParameterizedType
                ? resolveGenerics((ParameterizedType) actual, rootGenerics) : EMPTY_MAP;
        final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics =
                new HashMap<Class<?>, LinkedHashMap<String, Type>>();
        knownGenerics.put(middleType, typeGenerics);

        // root type
        typeGenerics = asType.getTypeParameters().length > 0
                // special case: root class also contains generics
                ? resolveRawGenerics(asType.getTypeParameters())
                : EMPTY_MAP;
        return create(asType, typeGenerics, knownGenerics, ignoreClasses);
    }


    private static GenericsInfo create(
            final Class type,
            final LinkedHashMap<String, Type> rootGenerics,
            final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics,
            final Class<?>... ignoreClasses) {

        final Map<Class<?>, LinkedHashMap<String, Type>> generics =
                new HashMap<Class<?>, LinkedHashMap<String, Type>>();
        generics.put(type, rootGenerics);

        analyzeType(generics,
                knownGenerics == null ? Collections.<Class<?>, LinkedHashMap<String, Type>>emptyMap() : knownGenerics,
                type,
                Arrays.asList(ignoreClasses));
        return new GenericsInfo(type, generics, ignoreClasses);
    }

    private static void analyzeType(final Map<Class<?>, LinkedHashMap<String, Type>> types,
                                    final Map<Class<?>, LinkedHashMap<String, Type>> knownTypes,
                                    final Class<?> type,
                                    final List<Class<?>> ignoreClasses) {
        Class<?> supertype = type;
        while (true) {
            for (Type iface : supertype.getGenericInterfaces()) {
                analyzeInterface(types, knownTypes, iface, supertype, ignoreClasses);
            }
            final Class next = supertype.getSuperclass();
            if (next == null || Object.class == next || ignoreClasses.contains(next)) {
                break;
            }
            // possibly provided generics (externally)
            types.put(next, knownTypes.containsKey(next)
                    ? knownTypes.get(next)
                    : analyzeParent(supertype, types.get(supertype)));
            supertype = next;
        }
    }

    private static void analyzeInterface(final Map<Class<?>, LinkedHashMap<String, Type>> types,
                                         final Map<Class<?>, LinkedHashMap<String, Type>> knownTypes,
                                         final Type iface,
                                         final Class<?> supertype,
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
                        resolveGenerics(parametrization, types.get(supertype));

                // no generics case and same resolved generics are ok (even if types in different branches of hierarchy)
                if (types.containsKey(interfaceType) && !generics.equals(types.get(interfaceType))) {
                    throw new IllegalStateException(String.format(
                            "Duplicate interface %s declaration in hierarchy: "
                                    + "can't properly resolve generics.", interfaceType.getName()));
                }
                types.put(interfaceType, generics);
            } else if (interfaceType.getTypeParameters().length > 0) {
                // root class didn't declare generics
                types.put(interfaceType, resolveRawGenerics(interfaceType.getTypeParameters()));
            } else if (!GROOVY_OBJECT.equals(interfaceType.getSimpleName())) {
                // avoid groovy specific interface (all groovy objects implements it)
                types.put(interfaceType, EMPTY_MAP);
            }
            analyzeType(types, knownTypes, interfaceType, ignoreClasses);
        }
    }

    private static LinkedHashMap<String, Type> analyzeParent(final Class type,
                                                             final Map<String, Type> rootGenerics) {
        LinkedHashMap<String, Type> generics = null;
        final Class parent = type.getSuperclass();
        if (!type.isInterface() && parent != null && parent != Object.class
                && type.getGenericSuperclass() instanceof ParameterizedType) {
            generics = resolveGenerics((ParameterizedType) type.getGenericSuperclass(), rootGenerics);
        } else if (parent != null && parent.getTypeParameters().length > 0) {
            // root class didn't declare generics
            generics = resolveRawGenerics(parent.getTypeParameters());
        }
        return generics == null ? EMPTY_MAP : generics;
    }

    private static LinkedHashMap<String, Type> resolveGenerics(final ParameterizedType type,
                                                               final Map<String, Type> rootGenerics) {
        final LinkedHashMap<String, Type> generics = new LinkedHashMap<String, Type>();
        final Type[] genericTypes = type.getActualTypeArguments();
        final Class interfaceType = (Class) type.getRawType();
        final TypeVariable[] genericNames = interfaceType.getTypeParameters();

        final int cnt = genericNames.length;
        for (int i = 0; i < cnt; i++) {
            final Type resolvedGenericType = GenericsUtils.resolveTypeGenerics(genericTypes[i], rootGenerics);
            generics.put(genericNames[i].getName(), resolvedGenericType);
        }
        return generics;
    }

    private static LinkedHashMap<String, Type> resolveRawGenerics(
            final TypeVariable... declaredGenerics) {
        final LinkedHashMap<String, Type> generics = new LinkedHashMap<String, Type>();
        for (TypeVariable type : declaredGenerics) {
            generics.put(type.getName(), GenericsUtils.resolveTypeGenerics(type.getBounds()[0], generics));
        }
        return generics;
    }
}

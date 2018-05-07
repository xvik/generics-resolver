package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.GenericsContext;
import ru.vyarus.java.generics.resolver.context.GenericsInfo;
import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;

import java.lang.reflect.*;
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
        return create(null, type, ignoreClasses);
    }

    /**
     * Type analysis in context of analyzed type. For example, resolution of field type class in context of
     * analyzed class (so we can correctly resolve it's generics). The result is not intended to be cached as it's
     * context-sensitive (when context provided).
     *
     * @param context       generics context of containing class (may be null for raw class analysis)
     * @param type          type to analyze (important: this must be generified type and not raw class in
     *                      order to properly resolve generics)
     * @param ignoreClasses classes to exclude from hierarchy analysis
     * @return analyzed type generics info
     */
    public static GenericsInfo create(
            final GenericsContext context, final Type type, final Class<?>... ignoreClasses) {
        if (context == null && type.getClass() != Class.class) {
            throw new IllegalStateException("Only direct class could be analyzed outer generics context");
        }

        final Map<Class<?>, LinkedHashMap<String, Type>> generics =
                new HashMap<Class<?>, LinkedHashMap<String, Type>>();

        final Class exactType;
        if (context == null) {
            // no outer context - provided type must be class and it's generics (possible) can't be completely resolved
            exactType = (Class) type;
            generics.put(exactType, exactType.getTypeParameters().length > 0
                    // special case: root class also contains generics
                    ? resolveRawGenerics(exactType.getTypeParameters())
                    : EMPTY_MAP);
        } else {
            // provided type in context of outer generics could be completely resolved
            exactType = context.resolveClass(type);
            generics.put(exactType, type instanceof ParameterizedType
                    ? resolveGenerics((ParameterizedType) type, context.genericsMap()) : EMPTY_MAP);
        }

        analyzeType(generics, exactType, Arrays.asList(ignoreClasses));
        return new GenericsInfo(exactType, generics, ignoreClasses);
    }

    private static void analyzeType(final Map<Class<?>, LinkedHashMap<String, Type>> types, final Class<?> type,
                                    final List<Class<?>> ignoreClasses) {
        Class<?> supertype = type;
        while (true) {
            for (Type iface : supertype.getGenericInterfaces()) {
                analyzeInterface(types, iface, supertype, ignoreClasses);
            }
            final Class next = supertype.getSuperclass();
            if (next == null || Object.class == next || ignoreClasses.contains(next)) {
                break;
            }
            types.put(next, analyzeParent(supertype, types.get(supertype)));
            supertype = next;
        }
    }

    private static void analyzeInterface(final Map<Class<?>, LinkedHashMap<String, Type>> types, final Type iface,
                                         final Class<?> supertype, final List<Class<?>> ignoreClasses) {
        final Class interfaceType = iface instanceof ParameterizedType
                ? (Class) ((ParameterizedType) iface).getRawType()
                : (Class) iface;
        if (!ignoreClasses.contains(interfaceType)) {
            if (iface instanceof ParameterizedType) {
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
            analyzeType(types, interfaceType, ignoreClasses);
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
            final Type resolvedGenericType = resolveActualType(genericTypes[i], rootGenerics);
            generics.put(genericNames[i].getName(), resolvedGenericType);
        }
        return generics;
    }

    private static Type resolveActualType(final Type genericType, final Map<String, Type> rootGenerics) {
        Type resolvedGenericType = null;
        if (genericType instanceof TypeVariable) {
            // simple named generics resolved to target types
            resolvedGenericType = rootGenerics.get(((TypeVariable) genericType).getName());
        } else if (genericType instanceof Class) {
            resolvedGenericType = genericType;
        } else if (genericType instanceof ParameterizedType) {
            final ParameterizedType parametrizedType = (ParameterizedType) genericType;
            resolvedGenericType = new ParameterizedTypeImpl(parametrizedType.getRawType(),
                    resolve(parametrizedType.getActualTypeArguments(), rootGenerics), parametrizedType.getOwnerType());
        } else if (genericType instanceof GenericArrayType) {
            final GenericArrayType arrayType = (GenericArrayType) genericType;
            resolvedGenericType = new GenericArrayTypeImpl(resolveActualType(
                    arrayType.getGenericComponentType(), rootGenerics));
        } else if (genericType instanceof WildcardType) {
            final WildcardType wildcard = (WildcardType) genericType;
            resolvedGenericType = new WildcardTypeImpl(resolve(wildcard.getUpperBounds(), rootGenerics),
                    resolve(wildcard.getLowerBounds(), rootGenerics));
        }
        return resolvedGenericType;
    }

    private static Type[] resolve(final Type[] types, final Map<String, Type> rootGenerics) {
        final Type[] resolved = new Type[types.length];
        for (int i = 0; i < types.length; i++) {
            resolved[i] = resolveActualType(types[i], rootGenerics);
        }
        return resolved;
    }

    private static LinkedHashMap<String, Type> resolveRawGenerics(
            final TypeVariable... declaredGenerics) {
        final LinkedHashMap<String, Type> generics = new LinkedHashMap<String, Type>();
        for (TypeVariable type : declaredGenerics) {
            generics.put(type.getName(), resolveActualType(type.getBounds()[0], generics));
        }
        return generics;
    }
}

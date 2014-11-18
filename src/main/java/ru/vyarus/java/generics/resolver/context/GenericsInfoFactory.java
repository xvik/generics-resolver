package ru.vyarus.java.generics.resolver.context;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Analyze class hierarchy and produce class hierarchy resolved generics object.
 * Resolved generics descriptors are cached.
 * <p>Note: when ignore classes used, cache will not work: such descriptors are always resolved.</p>
 *
 * @author Vyacheslav Rusakov
 * @since 16.10.2014
 */
public final class GenericsInfoFactory {

    private static final Map<Class<?>, GenericsInfo> CACHE = new WeakHashMap<Class<?>, GenericsInfo>();
    // lock will not affect performance for cached descriptors, just to make sure nothing was build two times
    private static final ReentrantLock LOCK = new ReentrantLock();
    @SuppressWarnings("PMD.LooseCoupling")
    private static final LinkedHashMap<String, Type> EMPTY_MAP = new LinkedHashMap<String, Type>(0);
    private static final String GROOVY_OBJECT = "GroovyObject";

    private GenericsInfoFactory() {
    }

    /**
     * Note: ignore classes switch off caching for resolved descriptor (and if completely resolved version
     * contained in cache limited version will be composed one more time).
     *
     * @param type          finder type to investigate
     * @param ignoreClasses list of classes to ignore during inspection (useful to avoid interface clashes)
     * @return descriptor for class hierarchy generics substitution
     */
    public static GenericsInfo create(final Class<?> type, final Class<?>... ignoreClasses) {
        GenericsInfo descriptor = ignoreClasses.length > 0 ? buildDescriptor(type, ignoreClasses) : CACHE.get(type);
        if (descriptor == null) {
            LOCK.lock();
            try {
                if (CACHE.get(type) != null) {
                    // descriptor could be created while thread wait for lock
                    descriptor = CACHE.get(type);
                } else {
                    descriptor = buildDescriptor(type);
                    // internal check
                    if (CACHE.get(type) != null) {
                        throw new IllegalStateException("Bad concurrency: descriptor already present in cache");
                    }
                    CACHE.put(type, descriptor);
                }
            } finally {
                LOCK.unlock();
            }
        }
        return descriptor;
    }

    private static GenericsInfo buildDescriptor(final Class<?> type, final Class<?>... ignoreClasses) {
        final Map<Class<?>, LinkedHashMap<String, Type>> generics =
                new HashMap<Class<?>, LinkedHashMap<String, Type>>();
        analyzeType(generics, type, Arrays.asList(ignoreClasses));
        return new GenericsInfo(type, generics);
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
            } else if (!GROOVY_OBJECT.equals(interfaceType.getSimpleName())) {
                // avoid groovy specific interface (all groovy objects implements it)
                types.put(interfaceType, EMPTY_MAP);
            }
            analyzeType(types, interfaceType, ignoreClasses);
        }
    }

    // LinkedHashMap used instead of usual map to avoid accidental simple map usage (order is important!)
    @SuppressWarnings("PMD.LooseCoupling")
    private static LinkedHashMap<String, Type> analyzeParent(final Class type,
                                                             final Map<String, Type> rootGenerics) {
        LinkedHashMap<String, Type> generics = null;
        final Class parent = type.getSuperclass();
        if (!type.isInterface() && parent != null && parent != Object.class
                && type.getGenericSuperclass() instanceof ParameterizedType) {
            generics = resolveGenerics((ParameterizedType) type.getGenericSuperclass(), rootGenerics);
        }
        return generics == null ? EMPTY_MAP : generics;
    }

    @SuppressWarnings("PMD.LooseCoupling")
    private static LinkedHashMap<String, Type> resolveGenerics(final ParameterizedType type,
                                                               final Map<String, Type> rootGenerics) {
        final LinkedHashMap<String, Type> generics = new LinkedHashMap<String, Type>();
        final Type[] genericTypes = type.getActualTypeArguments();
        final Class interfaceType = (Class) type.getRawType();
        final TypeVariable[] genericNames = interfaceType.getTypeParameters();

        final int cnt = genericNames.length;
        for (int i = 0; i < cnt; i++) {
            final Type genericType = genericTypes[i];
            Type resolvedGenericType;
            if (genericType instanceof TypeVariable) {
                // simple named generics resolved to target types
                resolvedGenericType = rootGenerics.get(((TypeVariable) genericType).getName());
            } else {
                // composite generics passed as is
                resolvedGenericType = genericType;
            }
            generics.put(genericNames[i].getName(), resolvedGenericType);
        }
        return generics;
    }
}

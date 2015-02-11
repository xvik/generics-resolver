package ru.vyarus.java.generics.resolver.util;

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

    public static GenericsInfo create(final Class<?> type, final Class<?>... ignoreClasses) {
        final Map<Class<?>, LinkedHashMap<String, Type>> generics =
                new HashMap<Class<?>, LinkedHashMap<String, Type>>();
        if (type.getTypeParameters().length > 0) {
            // special case: root class also contains generics
            generics.put(type, resolveRawGenerics(type.getTypeParameters()));
        } else {
            generics.put(type, EMPTY_MAP);
        }
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
            generics.put(type.getName(), resolveActualType(type.getBounds()[0], Collections.<String, Type>emptyMap()));
        }
        return generics;
    }
}

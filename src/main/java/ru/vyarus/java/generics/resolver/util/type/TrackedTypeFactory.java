package ru.vyarus.java.generics.resolver.util.type;

import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.ArrayTypeUtils;
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils;
import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.*;

/**
 * Tracks generics from known middle type.
 *
 * @author Vyacheslav Rusakov
 * @since 30.03.2019
 */
public final class TrackedTypeFactory {

    private TrackedTypeFactory() {
    }

    /**
     * Sub type must be either {@link ParameterizedType} or {@link WildcardType} containing parameterized types,
     * because otherwise tracking is impossible (no source to track from) and so orignial type is simply returned as is.
     * <p>
     * In case of arrays, internal components are directly compared.
     *
     * @param type   type to track generics for
     * @param source type to track generics from (middle type)
     * @return improved type or original type itself if nothing to track
     * @throws IllegalArgumentException if type is not assignable for provided sub type
     */
    public static Type build(final Type type, final Type source) {
        if (!TypeUtils.isAssignable(type, source)) {
            throw new IllegalArgumentException(String.format(
                    "Can't track type %s generics because it's not assignable to %s",
                    TypeToStringUtils.toStringType(type),
                    TypeToStringUtils.toStringTypeIgnoringVariables(source)));
        }

        Type res = type;

        if (ArrayTypeUtils.isArray(type) && ArrayTypeUtils.isArray(source)) {
            // for arrays track actual component types
            res = ArrayTypeUtils.toArrayType(
                    build(ArrayTypeUtils.getArrayComponentType(type), ArrayTypeUtils.getArrayComponentType(source)));
        } else {
            final Class<?> target = GenericsUtils.resolveClass(type);
            // it make sense to track from direct parameterized type or parameterized types inside wildcard
            if (target.getTypeParameters().length > 0
                    && (source instanceof ParameterizedType || source instanceof WildcardType)) {

                // select the most specific generics
                final Map<String, Type> generics = resolveGenerics(type, target, source);
                // empty generics may appear from wildcard without parameterized types (no source)
                if (!generics.isEmpty()) {
                    res = new ParameterizedTypeImpl(target,
                            generics.values().toArray(new Type[0]),
                            // intentionally not tracking owner type as redundant complication
                            TypeUtils.getOuter(type));
                }
            }
        }

        return res;
    }

    private static Map<String, Type> resolveGenerics(final Type type, final Class<?> target, final Type source) {
        final List<Map<String, Type>> selection = new ArrayList<>();

        if (source instanceof WildcardType) {
            for (Type sub : ((WildcardType) source).getUpperBounds()) {
                // use all parameterized types in wildcard
                if (sub instanceof ParameterizedType) {
                    selection.add(trackGenerics(target, (ParameterizedType) sub));
                }
            }
        } else {
            selection.add(trackGenerics(target, (ParameterizedType) source));
        }

        // in wildcard case may be no trackable source
        if (!selection.isEmpty() && type instanceof ParameterizedType) {
            // original type could already contain more specific generics
            selection.add(GenericsResolutionUtils.resolveGenerics(type, EmptyGenericsMap.getInstance()));
        }

        // select the most specific generics
        return mergeGenerics(selection);
    }

    private static Map<String, Type> trackGenerics(final Class<?> target, final ParameterizedType source) {
        final Class<?> middle = GenericsUtils.resolveClass(source);
        LinkedHashMap<String, Type> generics =
                GenericsResolutionUtils.resolveGenerics(source, EmptyGenericsMap.getInstance());
        if (!target.equals(middle)) {
            // for different types perform tracking
            generics = GenericsTrackingUtils.track(target, middle, generics);
        }

        // filter possible owner class generics
        return GenericsUtils.extractTypeGenerics(target, generics);
    }

    private static Map<String, Type> mergeGenerics(final List<Map<String, Type>> generics) {
        if (generics.isEmpty()) {
            return Collections.emptyMap();
        }
        final LinkedHashMap<String, Type> res = new LinkedHashMap<>();
        // they all will contain the same set of keys and in correct order
        for (String key : generics.get(0).keySet()) {
            Type type = null;
            for (Map<String, Type> map : generics) {
                final Type tmp = map.get(key);
                if (type == null || TypeUtils.isMoreSpecific(tmp, type)) {
                    type = tmp;
                }
            }
            res.put(key, type);
        }
        return res;
    }
}

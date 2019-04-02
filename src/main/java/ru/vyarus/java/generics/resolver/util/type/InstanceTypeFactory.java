package ru.vyarus.java.generics.resolver.util.type;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.type.instance.GenericArrayInstanceType;
import ru.vyarus.java.generics.resolver.util.type.instance.ParameterizedInstanceType;
import ru.vyarus.java.generics.resolver.util.type.instance.WildcardInstanceType;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Instance analysis logic for {@link ru.vyarus.java.generics.resolver.util.type.instance.InstanceType} construction.
 *
 * @author Vyacheslav Rusakov
 * @see ru.vyarus.java.generics.resolver.util.type.instance.InstanceType
 * @see ParameterizedInstanceType
 * @see GenericArrayInstanceType
 * @see WildcardInstanceType
 * @since 26.03.2019
 */
public final class InstanceTypeFactory {

    private InstanceTypeFactory() {
    }

    /**
     * Construct {@link ru.vyarus.java.generics.resolver.util.type.instance.InstanceType} for provided instance(s).
     * <p>
     * Returned type may not be an instance type, if further analysis is impossible:
     * <ul>
     * <li>If all instances are null {@code Object} returned</li>
     * <li>If instance is empty array then array class returned</li>
     * <li>If multiple empty arrays provided then median array type returned (for primitive arrays it would
     * always be wrapper class)</li>
     * </ul>
     *
     * @param instances instances to build type for
     * @return instance type if non null instances present or simple class (arrays class or just {@code Object})
     */
    public static Type build(final Object... instances) {
        final Object[] objects = filterNulls(instances);

        if (objects.length == 0) {
            // null assignable to anything (as Object in assignability logic)
            return Object.class;
        }

        final Type median = getMedianType(objects);
        final Class<?> type = GenericsUtils.resolveClass(median);

        return type.isArray() ? buildArrayType(median, instances) : buildType(median, objects);
    }

    private static Object[] filterNulls(final Object... objects) {
        boolean repackage = false;
        final Object[] res;
        for (Object obj : objects) {
            if (obj == null) {
                repackage = true;
                break;
            }
        }

        // no redundant object creation
        if (!repackage) {
            res = objects;
        } else {
            final List<Object> elts = new ArrayList<Object>();
            for (Object obj : objects) {
                if (obj != null) {
                    elts.add(obj);
                }
            }
            res = elts.toArray();
        }

        return res;
    }

    private static Type getMedianType(final Object... instances) {
        Type median = null;

        for (Object obj : instances) {
            final Class<?> type = obj.getClass();
            if (median == null) {
                median = type;
            } else {
                median = TypeUtils.getCommonType(median, type);
            }
        }
        return median;
    }

    private static Type buildArrayType(final Type median, final Object... objects) {
        // search for non empty arrays
        final List<Object[]> arrays = new ArrayList<Object[]>();
        final List<Object> elements = new ArrayList<Object>();
        for (Object object : objects) {
            final Object[] arr = filterNulls((Object[]) object);
            if (arr.length > 0) {
                arrays.add(arr);
                Collections.addAll(elements, arr);
            }
        }
        if (arrays.isEmpty()) {
            // when no instances available (inside arrays) we can return only class type
            // (nothing interesting in empty arrays instances)
            return median;
        }

        // Component type is a median type for all elements in all arrays
        final Type componentType = build(elements.toArray());

        return new GenericArrayInstanceType(componentType, arrays.toArray());
    }

    private static Type buildType(final Type median, final Object... objects) {
        if (median instanceof WildcardType && ((WildcardType) median).getUpperBounds().length > 1) {
            // using only the first type as instance type and other types will remain as simple correcting types
            // (no type info lost)
            final Type[] upperBounds = ((WildcardType) median).getUpperBounds();
            upperBounds[0] = new ParameterizedInstanceType(upperBounds[0], objects);
            return new WildcardInstanceType(upperBounds, objects);
        }
        // even if there will be wildcard, internal logic will use the first upper bound type only
        return new ParameterizedInstanceType(median, objects);
    }
}

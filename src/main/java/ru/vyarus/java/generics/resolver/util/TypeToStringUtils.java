package ru.vyarus.java.generics.resolver.util;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Builds string representation for type in context of generified class.
 *
 * @author Vyacheslav Rusakov
 * @since 17.11.2014
 */
public final class TypeToStringUtils {

    private TypeToStringUtils() {
    }

    /**
     * @param type     type to convert to string
     * @param generics type class generics type
     * @return string representation of provided type
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public static String toStringType(final Type type, final Map<String, Type> generics) {
        String res;
        if (type instanceof Class) {
            res = ((Class) type).getSimpleName();
        } else if (type instanceof ParameterizedType) {
            final ParameterizedType parametrized = (ParameterizedType) type;
            res = toStringType(parametrized.getRawType(), generics);
            final List<String> args = new ArrayList<String>();
            for (Type t : parametrized.getActualTypeArguments()) {
                args.add(toStringType(t, generics));
            }
            if (!args.isEmpty()) {
                res += "<" + join(args) + ">";
            }
        } else if (type instanceof GenericArrayType) {
            res = toStringType(((GenericArrayType) type).getGenericComponentType(), generics) + "[]";
        } else {
            // deep generics nesting case
            res = toStringType(generics.get(((TypeVariable) type).getName()), generics);
        }
        return res;
    }

    private static String join(final List<?> args) {
        final Iterator iterator = args.iterator();

        final Object first = iterator.next();
        String res;
        if (!iterator.hasNext()) {
            res = String.valueOf(first);
        } else {

            final StringBuilder buf = new StringBuilder(256);
            if (first != null) {
                buf.append(first);
            }

            while (iterator.hasNext()) {
                final Object obj = iterator.next();
                buf.append(", ").append(obj);
            }
            res = buf.toString();
        }
        return res;
    }
}

package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;

import java.lang.reflect.*;
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
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    public static String toStringType(final Type type, final Map<String, Type> generics) {
        final String res;
        if (type instanceof Class) {
            res = ((Class) type).getSimpleName();
        } else if (type instanceof ParameterizedType) {
            res = processParametrizedType((ParameterizedType) type, generics);
        } else if (type instanceof GenericArrayType) {
            res = toStringType(((GenericArrayType) type).getGenericComponentType(), generics) + "[]";
        } else if (type instanceof WildcardType) {
            res = processWildcardType((WildcardType) type, generics);
        } else {
            // deep generics nesting case
            res = toStringType(declaredGeneric((TypeVariable) type, generics), generics);
        }
        return res;
    }

    /**
     * Formats class as {@code Class<generics>}. Class is not checked to actually have generics!
     * Intended to be used for error reporting.
     *
     * @param type     class class to print with generics
     * @param generics known generics map class generics map
     * @return generified class string
     */
    public static String toStringClassWithGenerics(final Class<?> type, final Map<String, Type> generics) {
        return toStringType(new ParameterizedTypeImpl(type, generics.values().toArray(new Type[0])),
                generics);
    }

    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    private static String processParametrizedType(final ParameterizedType parametrized,
                                                  final Map<String, Type> generics) {
        String res = toStringType(parametrized.getRawType(), generics);
        final List<String> args = new ArrayList<String>();
        for (Type t : parametrized.getActualTypeArguments()) {
            args.add(toStringType(t, generics));
        }
        if (!args.isEmpty()) {
            res += "<" + join(args) + ">";
        }
        return res;
    }

    private static String processWildcardType(final WildcardType wildcard, final Map<String, Type> generics) {
        final String res;
        if (wildcard.getLowerBounds().length == 0) {
            res = "? extends " + toStringType(
                    GenericsUtils.resolveClass(wildcard.getUpperBounds()[0], generics), generics);
        } else {
            res = "? super " + toStringType(
                    GenericsUtils.resolveClass(wildcard.getLowerBounds()[0], generics), generics);
        }
        return res;
    }

    private static String join(final List<?> args) {
        final Iterator iterator = args.iterator();

        final Object first = iterator.next();
        final String res;
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

    private static Type declaredGeneric(final TypeVariable generic, final Map<String, Type> declarations) {
        final String name = generic.getName();
        final Type result = declarations.get(name);
        if (result == null) {
            throw new UnknownGenericException(name);
        }
        return result;
    }
}

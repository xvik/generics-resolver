package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.error.UnknownGenericException;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.*;
import java.util.*;

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
     * @see ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap to print not known generic names
     * @see ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap to print Object instead of not known generic
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
        } else if (type instanceof PrintableTypeVariable) {
            // print generic name (only when PrintableGenericsMap used)
            res = type.toString();
        } else {
            // deep generics nesting case
            // when PrintableGenericsMap used and generics is not known, will print generic name (see above)
            res = toStringType(declaredGeneric((TypeVariable) type, generics), generics);
        }
        return res;
    }

    /**
     * Print class with generic variables. For example, {@code List<T>}.
     *
     * @param type class to print
     * @return string containing class and it's declared generics
     */
    public static String toStringWithNamedGenerics(final Class<?> type) {
        return toStringType(new ParameterizedTypeImpl(type, type.getTypeParameters()), new PrintableGenericsMap());
    }

    /**
     * Formats class as {@code Class<generics>}. Only actual class generics are rendered.
     * Intended to be used for error reporting.
     *
     * @param type     class class to print with generics
     * @param generics known generics map class generics map
     * @return generified class string
     * @see ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap to print not known generic names
     * @see ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap to print Object instead of not known generic
     */
    public static String toStringWithGenerics(final Class<?> type, final Map<String, Type> generics) {
        // provided generics may contain outer type generics, but we will render only required generics
        final Map<String, Type> actual = type.getTypeParameters().length == generics.size()
                ? generics : GenericsUtils.getSelfGenerics(generics, GenericsUtils.getOwnerGenerics(type, generics));
        return toStringType(new ParameterizedTypeImpl(type, actual.values().toArray(new Type[0])), actual);
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

    /**
     * Special type, used only for Type to string conversion in order to preserve generic name.
     * Use by wrapping generics in {@link ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap}.
     */
    public static class PrintableTypeVariable implements Type {
        private final String name;

        public PrintableTypeVariable(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }
}

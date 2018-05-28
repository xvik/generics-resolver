package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.error.UnknownGenericException;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

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

    private static final String SPLIT = ", ";

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
        } else if (type instanceof ExplicitTypeVariable) {
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
        final Map<String, Type> actual = GenericsUtils.extractTypeGenerics(type, generics);
        return toStringType(new ParameterizedTypeImpl(type, actual.values().toArray(new Type[0])), actual);
    }

    /**
     * <pre>{@code class B extends A<Long> {}
     * class A<T> {
     *      List<T> get(T one);
     * }
     *
     * Method method = A.class.getMethod("get", Object.class);
     * Map<String, Type> generics = (context of B).method().visibleGenericsMap();
     * TypeToStringUtils.toStringMethod(method, generics) == "List<Long> get(Long)"
     * }</pre>.
     *
     * @param method method
     * @param generics required generics (type generics and possible method generics)
     * @return method string with replaced generic variables
     * @see ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap to print not known generic names
     * @see ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap to print Object instead of not known generic
     */
    public static String toStringMethod(final Method method, final Map<String, Type> generics) {
        return String.format("%s %s%s",
                toStringType(method.getGenericReturnType(), generics),
                method.getName(),
                toStringMethodParameters(method, generics));
    }

    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    private static String processParametrizedType(final ParameterizedType parametrized,
                                                  final Map<String, Type> generics) {
        final StringBuilder res = new StringBuilder(50);
        // important to cover potential owner type generics declaration (Owner<String>.Inner<Integer>)
        final Type outer = TypeUtils.getOuter(parametrized);
        if (outer != null) {
            // known outer generics will be contained, but in case of name clash (invisible outer generics)
            // use raw object)
            res.append(toStringType(outer, new IgnoreGenericsMap(generics)));
        }
        res.append(toStringType(parametrized.getRawType(), generics));
        final List<String> args = new ArrayList<String>();
        for (Type t : parametrized.getActualTypeArguments()) {
            args.add(toStringType(t, generics));
        }
        if (!args.isEmpty()) {
            res.append('<').append(join(args)).append('>');
        }
        return res.toString();
    }

    private static String processWildcardType(final WildcardType wildcard, final Map<String, Type> generics) {
        final String res;
        if (wildcard.getLowerBounds().length == 0) {
            // could be multiple bounds, because of stored named generic bounds (<T extends A & B>)
            // see GenericsResolutionUtils.resolveRawGeneric()
            final StringBuilder bounds = new StringBuilder(wildcard.getUpperBounds().length * 10);
            boolean first = true;
            for (Type type : wildcard.getUpperBounds()) {
                if (!first) {
                    bounds.append(" & ");
                }
                bounds.append(toStringType(GenericsUtils.resolveClass(type, generics), generics));
                first = false;
            }
            res = "? extends " + bounds.toString();
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
                buf.append(SPLIT).append(obj);
            }
            res = buf.toString();
        }
        return res;
    }

    private static String toStringMethodParameters(final Method method, final Map<String, Type> generics) {
        final int count = method.getParameterTypes().length;
        final StringBuilder res = new StringBuilder(count * 10 + 2)
                .append("(");
        if (count > 0) {
            boolean first = true;
            for (Type type : method.getGenericParameterTypes()) {
                if (!first) {
                    res.append(SPLIT);
                }
                res.append(toStringType(type, generics));
                first = false;
            }
        }
        return res.append(")").toString();
    }

    private static Type declaredGeneric(final TypeVariable generic, final Map<String, Type> declarations) {
        final String name = generic.getName();
        final Type result = declarations.get(name);
        // last condition to prevent infinite cycle (should be impossible case)
        if (result == null || result instanceof TypeVariable) {
            throw new UnknownGenericException(name, generic.getGenericDeclaration());
        }
        return result;
    }
}

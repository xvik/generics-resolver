package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.error.UnknownGenericException;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.*;
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
     * Shortcut for {@link #toStringType(Type, Map)} (called with {@link IgnoreGenericsMap}). Could be used
     * when class must be resolved ignoring possible variables.
     * <p>
     * Note: {@code Object} will be used instead of variable even if it has upper bound declared (e.g.
     * {@code T extends Serializable}).
     *
     * @param type type type to convert to string
     * @return string representation of provided type
     */
    public static String toStringTypeIgnoringVariables(final Type type) {
        return toStringType(type, EmptyGenericsMap.getInstance());
    }

    /**
     * Shortcut for {@link #toStringType(Type, Map)} (called with {@link EmptyGenericsMap}). Could be used
     * when provided type does not contain variables. If provided type contain variables, error will be thrown.
     *
     * @param type type to convert to string
     * @return string representation of provided type
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     */
    public static String toStringType(final Type type) {
        return toStringType(type, EmptyGenericsMap.getInstance());
    }

    /**
     * Prints type as string. E.g. {@code toStringType(ParameterizedType(List, String), [:]) == "List<String>"},
     * {@code toStringType(WildcardType(String), [:]) == "? extends String" }.
     * <p>
     * If {@link ParameterizedType} is inner class and contains information about outer generics, it will be printed
     * as {@code Outer<Generics>.Inner<Generics>} in order to indicate all available information.
     * In other cases outer class is not indicated.
     *
     * @param type     type to convert to string
     * @param generics type class generics type
     * @return string representation of provided type
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap to print not known generic names
     * @see #toStringTypeIgnoringVariables(Type) shortcut to print Object instead of not known generic
     * @see #toStringType(Type) shortcut for types without variables
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
     * <p>
     * Note: if class is inned class, outer class is not printed!
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
     * @param method   method
     * @param generics required generics (type generics and possible method generics)
     * @return method string with replaced generic variables
     * @see ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap to print not known generic names
     * @see ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap to print Object instead of not known generic
     */
    public static String toStringMethod(final Method method, final Map<String, Type> generics) {
        return String.format("%s %s(%s)",
                toStringType(method.getGenericReturnType(), generics),
                method.getName(),
                join(method.getGenericParameterTypes(), generics));
    }

    /**
     * <pre>{@code class B extends A<Long> {}
     * class A<T> {
     *      A(T arg);
     * }
     *
     * Constructor method = A.class.getConstructor(Object.class);
     * Map<String, Type> generics = (context of B).method().visibleGenericsMap();
     * TypeToStringUtils.toStringConstructor(constructor, generics) == "A(Long)"
     * }</pre>.
     *
     * @param constructor constructor
     * @param generics    required generics (type generics and possible constructor generics)
     * @return constructor string with replaced generic variables
     * @see ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap to print not known generic names
     * @see ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap to print Object instead of not known generic
     */
    public static String toStringConstructor(final Constructor constructor, final Map<String, Type> generics) {
        return String.format("%s(%s)",
                constructor.getDeclaringClass().getSimpleName(),
                join(constructor.getGenericParameterTypes(), generics));
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
            res.append(toStringType(outer, new IgnoreGenericsMap(generics))).append('.');
        }
        res.append(toStringType(parametrized.getRawType(), generics));
        final Type[] args = parametrized.getActualTypeArguments();
        if (args.length > 0) {
            final String params = join(args, generics);
            // do not print absent parametrization
            if (!params.replace(", ", "").replace("Object", "").isEmpty()) {
                res.append('<').append(params).append('>');
            }
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
                bounds.append(toStringType(type, generics));
                first = false;
            }
            res = "? extends " + bounds.toString();
        } else {
            res = "? super " + toStringType(wildcard.getLowerBounds()[0], generics);
        }
        return res;
    }

    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    private static String join(final Type[] args, final Map<String, Type> generics) {
        final String res;
        if (args.length == 0) {
            res = "";
        } else if (args.length == 1) {
            // only one argument
            res = toStringType(args[0], generics);
        } else {
            final StringBuilder buf = new StringBuilder(args.length * 20);
            boolean first = true;
            for (Type type : args) {
                if (!first) {
                    buf.append(", ");
                }
                buf.append(toStringType(type, generics));
                first = false;
            }
            res = buf.toString();
        }
        return res;
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

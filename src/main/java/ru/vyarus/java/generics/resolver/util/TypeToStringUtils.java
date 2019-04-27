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

    private static final String COMMA_SEPARATOR = ", ";
    private static final String HASH = "#";

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
            res = processClass((Class) type);
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
     * Shortcut for {@link #toStringTypes(Type[], String, Map)} with comma separator (most commonly used).
     *
     * @param types    types to convert to string
     * @param generics generics (common for all types)
     * @return string with all types divided by comma
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap to print not known generic names
     * @see ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap to print Object instead of not known generic
     * @see EmptyGenericsMap for no-generics case (to avoid creating empty map)
     */
    public static String toStringTypes(final Type[] types, final Map<String, Type> generics) {
        return toStringTypes(types, COMMA_SEPARATOR, generics);
    }

    /**
     * Calls {@link #toStringType(Type, Map)} for all types and join resulted strings with provided separator.
     * All types must be either without generics or from the single scope (same class or method) so provided generics
     * map contains generics for all types.
     * <p>
     * Useful for example, to format types in error message.
     *
     * @param types     types to convert to string
     * @param separator separator string
     * @param generics  generics (common for all types)
     * @return string with all types divided by separator
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see #toStringTypes(Type[], Map) shortcut for comma - separated types (most useful)
     */
    @SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
    public static String toStringTypes(final Type[] types,
                                       final String separator,
                                       final Map<String, Type> generics) {
        final String res;
        if (types.length == 0) {
            res = "";
        } else if (types.length == 1) {
            // only one argument
            res = toStringType(types[0], generics);
        } else {
            final StringBuilder buf = new StringBuilder(types.length * 20);
            boolean first = true;
            for (Type type : types) {
                if (!first) {
                    buf.append(separator);
                }
                buf.append(toStringType(type, generics));
                first = false;
            }
            res = buf.toString();
        }
        return res;
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
                toStringTypes(method.getGenericParameterTypes(), generics));
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
                toStringTypes(constructor.getGenericParameterTypes(), generics));
    }

    /**
     * For usual classes {@link Class#getSimpleName()} returned. For anonymous classes returning
     * "EnclosingClass#(constructor|method)$type", where EnclosingClass is a class where anonymous class was
     * declared, (constructor|method) is method or constructor signature where anonymous class declared
     * (not present if anonymous class declared in class field) and type is direct super type of anonymous class.
     * <p>
     * For example, for
     * <pre>{@code
     *      class Something {
     *          private Inner in1 = new Inner() {...};
     *
     *          void method() {
     *              Inner in2 = new Inner() {...}
     *          }
     *      }
     * }</pre>
     * to string for in1 will be "Something$Inner" and for in2 "Something#void method()$Inner.
     * <p>
     * Note that in java {@code getName()} for such classes would be like "com.mycompany.Something$1", which does
     * not shows all possible type information ({@code getSimpleName()} on anonymous class is empty string!).
     * <p>
     * In groovy, enclosing methods and constructors can't be resolved and so would not be included. Most certainly,
     * for groovy type (anonymous class created in groovy code, not in java), GroovyObject will appear instead of
     * Object (because every object in groovy implements GroovyObject).
     *
     * @param clazz class
     * @return to string class representation
     */
    @SuppressWarnings("PMD.UseStringBufferForStringAppends")
    private static String processClass(final Class clazz) {
        String res;
        // simpleName on anonymous class is empty string
        if (clazz.isAnonymousClass()) {
            res = clazz.getEnclosingClass().getSimpleName();
            try {
                if (clazz.getEnclosingConstructor() != null) {
                    res += HASH + toStringConstructor(clazz.getEnclosingConstructor(), IgnoreGenericsMap.getInstance());
                } else if (clazz.getEnclosingMethod() != null) {
                    res += HASH + toStringMethod(clazz.getEnclosingMethod(), IgnoreGenericsMap.getInstance());
                }
            } catch (Throwable ignored) {
                // ignore possible "enclosing method not found" in groovy
            }
            res += "$" + (clazz.getSuperclass() == Object.class && clazz.getInterfaces().length > 0
                    ? clazz.getInterfaces()[0].getSimpleName() : clazz.getSuperclass().getSimpleName());
        } else {
            res = clazz.getSimpleName();
        }
        return res;
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
            final String params = toStringTypes(args, generics);
            // do not print absent parametrization (it can't be checked before toString)
            if (!params.replace(COMMA_SEPARATOR, "").replace("Object", "").isEmpty()) {
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
            res = "? extends " + toStringTypes(wildcard.getUpperBounds(), " & ", generics);
        } else {
            res = "? super " + toStringType(wildcard.getLowerBounds()[0], generics);
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

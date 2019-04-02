package ru.vyarus.java.generics.resolver.util.type;

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl;
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;

/**
 * Convenient utility for types construction. Assumed to be used mostly in tests with static import
 * {@code import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*;}.
 * <p>
 * Returned types could be easily prettry printed as they all have toString implemented through
 * {@link ru.vyarus.java.generics.resolver.util.TypeToStringUtils}.
 *
 * @author Vyacheslav Rusakov
 * @since 02.04.2019
 */
public final class TypeFactory {

    private TypeFactory() {
    }

    /**
     * {@code param(List.class, String.class) == List<String>}.
     *
     * @param root      root class
     * @param arguments arguments (class generics
     * @return parameterized type
     */
    public static ParameterizedType param(final Class root, final Type... arguments) {
        return new ParameterizedTypeImpl(root, arguments);
    }

    /**
     * {@code param(SomeList.class, new Type[]{String.class}, param(Root.class, Some.class))
     * == Root<String>.SomeList<String>}.
     *
     * @param root      inner class
     * @param arguments inner class generics
     * @param owner     owner class (could be also generified)
     * @return generified inner class type
     */
    public static ParameterizedType param(final Class root, final Type[] arguments, final Type owner) {
        return new ParameterizedTypeImpl(root, arguments, owner);
    }

    /**
     * {@code array(param(List.class, String.class) == List<String>[]}.
     * <p>
     * For simple arrays use direct declaration: {@code String[]}
     *
     * @param type component type
     * @return array type for component
     */
    public static GenericArrayType array(final Type type) {
        return new GenericArrayTypeImpl(type);
    }

    /**
     * {@code upper(Number.class, Comparable.class) == ? extends Number & Comparable}.
     *
     * @param types wildcard upper bound types
     * @return wildcard type
     */
    public static WildcardType upper(final Type... types) {
        return WildcardTypeImpl.upper(types);
    }

    /**
     * {@code lower(String.class) == ? super String}.
     *
     * @param type lower bound type
     * @return wildcard type
     */
    public static WildcardType lower(final Type type) {
        return WildcardTypeImpl.lower(type);
    }

    /**
     * {@code literal(new L<List<String>>(){}) == List<String>}.
     * <p>
     * Method mostly exists to remind about {@link TypeLiteral}, which can be used directly like
     * {@code new TypeLiteral<List<String>>(){}.getType()}.
     *
     * @param literal literal with type declaration
     * @param <T>     actual type
     * @return type declared in literal
     */
    public static <T> Type literal(final L<T> literal) {
        if (literal.getClass().equals(L.class)) {
            throw new IllegalArgumentException(
                    "Incorrect usage: literal type must be an anonymous class: new L<Some>(){}");
        }
        return literal.getType();
    }

    /**
     * Shorter name for {@link TypeLiteral}.
     *
     * @param <T> type declaration
     */
    public static class L<T> extends TypeLiteral<T> {
    }
}

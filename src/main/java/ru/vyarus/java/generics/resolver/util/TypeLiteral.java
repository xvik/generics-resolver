package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Class inspired by guice {@code com.google.inject.TypeLiteral}. Used to easily declare types.
 * <p>
 * Usage: create empty anonymous class to capture type: {@code new TypeLiteral<List<String>>() {}}.
 * Use {@link #getType()} to access captured type.
 *
 * @param <T> type to declare
 * @author Vyacheslav Rusakov
 * @since 15.12.2018
 */
public class TypeLiteral<T> {
    private static final PrintableGenericsMap PRINTABLE_GENERICS = new PrintableGenericsMap();

    private final Type type;

    protected TypeLiteral() {
        type = readDeclaredType();
    }

    /**
     * @return type declared
     */
    public Type getType() {
        return type;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(type, PRINTABLE_GENERICS);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof TypeLiteral && ((TypeLiteral) obj).getType().equals(type);
    }

    private Type readDeclaredType() {
        final Type superclass = getClass().getGenericSuperclass();
        if (superclass instanceof Class) {
            throw new IllegalArgumentException("Missing type parameter.");
        }
        final ParameterizedType parameterized = (ParameterizedType) superclass;
        return parameterized.getActualTypeArguments()[0];
    }
}

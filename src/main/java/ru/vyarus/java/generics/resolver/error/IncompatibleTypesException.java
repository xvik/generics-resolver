package ru.vyarus.java.generics.resolver.error;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.Type;

/**
 * Exception thrown to indicate type incompatibility. Types may contain not resolved variable - they will be
 * shown in error message.
 *
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
public class IncompatibleTypesException extends GenericsException {
    private final Type first;
    private final Type second;

    /**
     * @param first  first type
     * @param second second type
     */
    public IncompatibleTypesException(final Type first, final Type second) {
        this(null, first, second);
    }

    /**
     * @param message message with 2 placeholders (%s) for formatted types
     * @param first   first type
     * @param second  second type
     */
    public IncompatibleTypesException(final String message, final Type first, final Type second) {
        super(formatMessage(message, first, second));
        this.first = first;
        this.second = second;
    }

    /**
     * @return first type
     */
    public Type getFirst() {
        return first;
    }

    /**
     * @return second type
     */
    public Type getSecond() {
        return second;
    }

    private static String formatMessage(final String message,
                                        final Type first,
                                        final Type second) {
        final String msg = message == null ? "Incompatible types: %s and %s" : message;
        final PrintableGenericsMap generics = new PrintableGenericsMap();
        return String.format(msg, TypeToStringUtils.toStringType(first, generics),
                TypeToStringUtils.toStringType(second, generics));
    }
}

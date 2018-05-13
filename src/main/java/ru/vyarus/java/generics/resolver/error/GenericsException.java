package ru.vyarus.java.generics.resolver.error;

/**
 * Base class for generic-related exceptions. May be used to intercept all generic analysis related exceptions
 * ({@code catch(GenericRelatedException ex)}).
 *
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
public abstract class GenericsException extends RuntimeException {

    public GenericsException(final String message) {
        super(message);
    }

    public GenericsException(final String message, final Throwable cause) {
        super(message, cause);
    }
}

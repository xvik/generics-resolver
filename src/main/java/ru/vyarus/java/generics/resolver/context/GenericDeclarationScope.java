package ru.vyarus.java.generics.resolver.context;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;

/**
 * Enum specifies possible generic declaration sources.
 *
 * @author Vyacheslav Rusakov
 * @since 30.05.2018
 */
public enum GenericDeclarationScope {
    /**
     * Generic declared on class: {@code Some<T>}.
     */
    CLASS,
    /**
     * Generic declared on method: {@code <T> T get()}.
     */
    METHOD,
    /**
     * Generic declared on constructor: {@code <T> Some(T arg)}.
     */
    CONSTRUCTOR;

    /**
     * Generics visibility compatibility. E.g. generics from CLASS scope are visible from METHOD scope (yes, method
     * or constructor generics could override class generics, but this edge case is ignored).
     *
     * @param scope scope to compare visibility
     * @return if compared to class scope or the same scope, false otherwise
     */
    public boolean isCompatible(final GenericDeclarationScope scope) {
        // null is impossible case for current java (no other sources)
        return scope != null && (scope == CLASS || scope == this);
    }

    /**
     * Note that currently all possible scopes are covered. Null case is for possible future cases.
     *
     * @param source declaration source
     * @return recognized scope or null
     */
    public static GenericDeclarationScope from(final GenericDeclaration source) {
        GenericDeclarationScope res = null;
        if (source != null) {
            if (source instanceof Class) {
                res = CLASS;
            } else if (source instanceof Method) {
                res = METHOD;
            } else if (source instanceof Constructor) {
                res = CONSTRUCTOR;
            }
        }
        return res;
    }
}

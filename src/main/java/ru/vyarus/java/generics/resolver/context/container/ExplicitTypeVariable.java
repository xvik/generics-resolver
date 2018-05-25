package ru.vyarus.java.generics.resolver.context.container;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.Arrays;

/**
 * Special implementation for holding generic name from {@link java.lang.reflect.TypeVariable}. Used when
 * generic must be preserved by all utilities (and no error thrown for unknown generic).
 * <p>
 * Used during generics tracing and type to string with named generics.
 *
 * @author Vyacheslav Rusakov
 * @since 24.05.2018
 */
public class ExplicitTypeVariable implements Type {

    private final String name;
    private final Object declarationSource;
    private final Type[] bounds;

    public ExplicitTypeVariable(final TypeVariable variable) {
        this.name = variable.getName();
        this.declarationSource = variable.getGenericDeclaration();
        this.bounds = variable.getBounds();
    }

    public ExplicitTypeVariable(final String name) {
        this.name = name;
        this.declarationSource = null;
        this.bounds = new Type[]{Object.class};
    }

    /**
     * @return variable name
     */
    public String getName() {
        return name;
    }

    /**
     * @return declaration class/method if available or null
     */
    public Object getDeclarationSource() {
        return declarationSource;
    }

    /**
     * @return variable bound (Object when no known bound)
     */
    public Type[] getBounds() {
        return Arrays.copyOf(bounds, bounds.length);
    }

    @Override
    public String toString() {
        return name;
    }
}

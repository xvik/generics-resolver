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
    private final TypeVariable declarationSource;
    private final Type[] bounds;

    public ExplicitTypeVariable(final TypeVariable variable) {
        this.name = variable.getName();
        this.declarationSource = variable;
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
     * @return original (source) type variable or null
     */
    public TypeVariable getDeclarationSource() {
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ExplicitTypeVariable)) {
            return false;
        }

        final ExplicitTypeVariable that = (ExplicitTypeVariable) o;

        if (!name.equals(that.name)) {
            return false;
        }
        if (declarationSource != null
                ? !declarationSource.equals(that.declarationSource) : that.declarationSource != null) {
            return false;
        }
        return Arrays.equals(bounds, that.bounds);
    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + (declarationSource != null ? declarationSource.hashCode() : 0);
        result = 31 * result + Arrays.hashCode(bounds);
        return result;
    }
}

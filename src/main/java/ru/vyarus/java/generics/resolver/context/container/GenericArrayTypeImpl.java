package ru.vyarus.java.generics.resolver.context.container;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collections;

/**
 * Wrapper to hold resolved array type.
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2014
 */
public class GenericArrayTypeImpl implements GenericArrayType {

    private final Type componentType;

    public GenericArrayTypeImpl(final Type componentType) {
        this.componentType = componentType;
    }

    @Override
    public Type getGenericComponentType() {
        return componentType;
    }

    @Override
    @SuppressWarnings({"checkstyle:needbraces", "PMD.IfStmtsMustUseBraces", "PMD.OnlyOneReturn"})
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof GenericArrayTypeImpl)) return false;

        final GenericArrayTypeImpl that = (GenericArrayTypeImpl) o;

        if (componentType != null
                ? !componentType.equals(that.componentType)
                : that.componentType != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return componentType != null ? componentType.hashCode() : 0;
    }

    @Override
    public String toString() {
        return TypeToStringUtils.toStringType(this, Collections.<String, Type>emptyMap());
    }
}

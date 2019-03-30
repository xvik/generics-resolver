package ru.vyarus.java.generics.resolver.util.type.instance;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;

/**
 * {@link InstanceType} container for {@link GenericArrayType} (or pure array). Holds known array type together
 * with original array instance(s).
 * <p>
 * Assumed to be created with {@link ru.vyarus.java.generics.resolver.util.type.InstanceTypeFactory} because
 * type itself does not contain type analysis logic.
 * <p>
 * Inner type must be resolved as median from all objects inside all array instances. This makes this type
 * near useless as component type always contain another {@link InstanceType}. But instance array type may be
 * useful in rare cases when it is important to differentiate original instance arrays (to properly calculate
 * component type generics).
 *
 * @author Vyacheslav Rusakov
 * @since 27.03.2019
 */
public class GenericArrayInstanceType implements GenericArrayType, InstanceType {

    private final Object[] instances;
    private final Type componentType;

    /**
     * Warning: this is container type and it does not perform type integrity checks!
     * Better create instance types through {@link ru.vyarus.java.generics.resolver.util.type.InstanceTypeFactory}.
     * Constructor remain open only for  possible case when it would be useful as holder in different context.
     * <p>
     * Component type will always be an {@link InstanceType} if type is constructed by factory. Non instance type
     * is also supported in case of possible future usages in different circumstances.
     *
     * @param componentType component type, already resolved from instances
     * @param instances     array instances used for analysis (without nulls)
     */
    public GenericArrayInstanceType(final Type componentType, final Object... instances) {
        if (instances.length == 0) {
            throw new IllegalArgumentException("No instances provided");
        }
        this.instances = instances;
        this.componentType = componentType;
    }

    @Override
    public Iterator iterator() {
        return Arrays.asList(instances).iterator();
    }

    @Override
    public Type getGenericComponentType() {
        return componentType;
    }

    @Override
    public Object getInstance() {
        return instances[0];
    }

    @Override
    public boolean hasMultipleInstances() {
        return instances.length > 1;
    }

    @Override
    public Object[] getAllInstances() {
        return Arrays.copyOf(instances, instances.length);
    }

    @Override
    public boolean isCompleteType() {
        return !(componentType instanceof InstanceType) || ((InstanceType) componentType).isCompleteType();
    }

    @Override
    public ParameterizedInstanceType getImprovableType() {
        return componentType instanceof InstanceType
                ? ((InstanceType) componentType).getImprovableType() : null;
    }

    @Override
    public boolean equals(final Object o) {
        boolean res = this == o;
        if (!res && o instanceof GenericArrayType) {
            final Type thatComponentType = ((GenericArrayType) o).getGenericComponentType();
            res = componentType.equals(thatComponentType);
        }
        return res;
    }

    @Override
    public int hashCode() {
        return componentType.hashCode();
    }

    @Override
    public String toString() {
        // append first instance hash code to uniquely identify type by contained instance
        return String.format("%s (%s%s)",
                TypeToStringUtils.toStringType(this),
                Integer.toHexString(getInstance().hashCode()),
                hasMultipleInstances() ? ",...(" + instances.length + ")" : "");
    }
}

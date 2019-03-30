package ru.vyarus.java.generics.resolver.util.type.instance;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;

import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.Arrays;
import java.util.Iterator;

/**
 * {@link InstanceType} container for {@link WildcardType}. Holds all known composing types and together with
 * original object instance(s).
 * <p>
 * Assumed to be created with {@link ru.vyarus.java.generics.resolver.util.type.InstanceTypeFactory} because
 * type itself does not contain type analysis logic.
 * <p>
 * Wildcard type are commonly appear during median type resolution from multiple instances
 * ({@link ru.vyarus.java.generics.resolver.util.TypeUtils#getCommonType(Type, Type)}). In this case, only the
 * first type (usually class) is taken to become another instance type (with the same set of instances as wildcard).
 * Wildcard is required to preserve additional composing types (without it some type information will be lost).
 * <p>
 * This means that this type is completely useless for instance type analysis, it just holds additional type
 * information (resolved on previous stages).
 *
 * @author Vyacheslav Rusakov
 * @since 29.03.2019
 */
public class WildcardInstanceType implements WildcardType, InstanceType {

    private final Type[] noLowerBounds = new Type[0];

    private final Object[] instances;
    private final Type[] upperBounds;

    /**
     * Warning: this is container type and it does not perform type integrity checks!
     * Better create instance types through {@link ru.vyarus.java.generics.resolver.util.type.InstanceTypeFactory}.
     * Constructor remain open only for  possible case when it would be useful as holder in different context.
     * <p>
     * First type will always be an {@link InstanceType} if type is constructed by factory. Non instance type
     * is also supported in case of possible future usages in different circumstances.
     *
     * @param upperBounds composing types resolved from instances (only the first type may be instance type!)
     * @param instances   array instances used for analysis (without nulls)
     * @throws IllegalArgumentException if no instances provided or bounds contain not first instance type
     */
    public WildcardInstanceType(final Type[] upperBounds, final Object... instances) {
        if (instances.length == 0) {
            throw new IllegalArgumentException("No instances provided");
        }
        if (upperBounds.length == 0) {
            throw new IllegalArgumentException("No upper bounds provided");
        }
        for (int i = 1; i < upperBounds.length; i++) {
            if (upperBounds[i] instanceof InstanceType) {
                throw new IllegalArgumentException(String.format(
                        "Only the first type could be an instance type but type %s is %s (all types: %s)",
                        i + 1, upperBounds[i].getClass(),
                        TypeToStringUtils.toStringTypes(upperBounds, EmptyGenericsMap.getInstance())));
            }
        }

        this.instances = instances;
        this.upperBounds = upperBounds;
    }

    @Override
    public Iterator iterator() {
        return Arrays.asList(instances).iterator();
    }

    @Override
    public Type[] getUpperBounds() {
        return Arrays.copyOf(upperBounds, upperBounds.length);
    }

    @Override
    public Type[] getLowerBounds() {
        // useless for type resolution
        return noLowerBounds;
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
        return !(upperBounds[0] instanceof InstanceType) || ((InstanceType) upperBounds[0]).isCompleteType();
    }

    @Override
    public ParameterizedInstanceType getImprovableType() {
        return upperBounds[0] instanceof InstanceType
                ? ((InstanceType) upperBounds[0]).getImprovableType() : null;
    }

    @Override
    public String toString() {
        // append first instance hash code to uniquely identify type by contained instance
        return TypeToStringUtils.toStringType(this) + " (" + Integer.toHexString(getInstance().hashCode())
                + (hasMultipleInstances() ? ",...(" + instances.length + ")" : "") + ")";
    }

    @Override
    public boolean equals(final Object o) {
        boolean res = this == o;
        if (!res && o instanceof WildcardType) {
            final WildcardType that = (WildcardType) o;
            final Type[] thatLowerBounds = that.getLowerBounds();
            final Type[] thatUpperBounds = that.getUpperBounds();

            res = Arrays.equals(noLowerBounds, thatLowerBounds) && Arrays.equals(upperBounds, thatUpperBounds);
        }
        return res;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(upperBounds);
        result = 31 * result + Arrays.hashCode(noLowerBounds);
        return result;
    }
}

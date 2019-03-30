package ru.vyarus.java.generics.resolver.util.type.instance;

import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;

/**
 * {@link InstanceType} container for {@link ParameterizedType}. Holds known type together with original instance(s).
 * Note that if contained type does not have generics it would still be {@link ParameterizedType} with empty generics
 * array because we have to preserve original instance and {@link Class} type is final.
 * <p>
 * Assumed to be created with {@link ru.vyarus.java.generics.resolver.util.type.InstanceTypeFactory} because
 * type itself does not contain type analysis logic.
 * <p>
 * Instance type extends {@link ParameterizedType} and behave like it (so may be used as pure type everywhere as pure
 * type). All directly available type information is resolved immediately (by using upper declaration bounds for
 * class generics). The completeness of type could be checked with {@link #isCompleteType()} which is initially true
 * only if type does not contain direct generics (obviously unknown).
 * <p>
 * Intended usage: use the type to wrap actual instance and then use it as usual type. Some parts of logic
 * may further investigate type if required. For example, if instance is {@link java.util.List} then
 * some lists support will extract contained elements and resolve median type of list generic and call
 * {@link #improveAccuracy(Type...)} in order to correct this type (note that provided type may also
 * be an instance type so next level analysis could further resolve type information).
 *
 * @author Vyacheslav Rusakov
 * @see GenericArrayInstanceType describing arrays which can't be expressed by parameterized type
 * @see WildcardInstanceType holding additional type information and allow further analysis
 * @since 14.03.2019
 */
public class ParameterizedInstanceType implements ParameterizedType, InstanceType {

    private final Object[] instances;
    private Class<?> rawType;
    private Type[] actualArguments;
    private Type ownerType;
    private boolean completeType;

    /**
     * Warning: this is container type and it does not perform type integrity checks!
     * Better create instance types through {@link ru.vyarus.java.generics.resolver.util.type.InstanceTypeFactory}.
     * Constructor remain open only for  possible case when it would be useful as holder in different context.
     *
     * @param type      type resolved from instances (in most cases simple class)
     * @param instances actual non null (!) instances used for analysis
     * @throws IllegalArgumentException if no instances provided
     */
    public ParameterizedInstanceType(final Type type, final Object... instances) {
        if (instances.length == 0) {
            throw new IllegalArgumentException("No instances provided");
        }
        this.instances = instances;
        analyze(type);
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.asList(instances).iterator();
    }

    @Override
    public Type[] getActualTypeArguments() {
        return Arrays.copyOf(actualArguments, actualArguments.length);
    }

    @Override
    public Class<?> getRawType() {
        return rawType;
    }

    @Override
    public Type getOwnerType() {
        return ownerType;
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
        return completeType;
    }

    @Override
    public ParameterizedInstanceType getImprovableType() {
        return this;
    }

    /**
     * Instance type itself can;t be used to properly guess generics, but external logic, knowing actual context, could
     * resolve generic types using deeper instance analysis. For example, it is possible in case of
     * {@link java.util.List} (or any other container): contained object types must be checked.
     * <p>
     * This method must be called if external logic could improve generic typings so all logic, already holding
     * reference to this instance could immediately benefit from more accurate type.
     * <p>
     * After improving accuracy, type assumed to be complete ({@link #isCompleteType()}).
     *
     * @param arguments more accurate types
     * @throws IllegalArgumentException if provided types are not correct (count) or contains less accurate types
     *                                  then already contained
     * @see #isMoreSpecificGenerics(Type...) to test arguments before
     */
    public void improveAccuracy(final Type... arguments) {
        if (!isMoreSpecificGenerics(arguments)) {
            throw new IllegalArgumentException(String
                    .format("Provided generics for type %s [%s] are less specific then current [%s]",
                            rawType.getSimpleName(),
                            TypeToStringUtils.toStringTypes(arguments, EmptyGenericsMap.getInstance()),
                            TypeToStringUtils.toStringTypes(actualArguments, EmptyGenericsMap.getInstance())));
        }
        this.actualArguments = arguments;
        this.completeType = true;
    }

    /**
     * Useful for types check before calling {@link #improveAccuracy(Type...)} which will fail if provided types
     * would be less specific (actually if at least one type is less specific).
     *
     * @param arguments more specific generics for current type
     * @return true if provided generics are more specific, false otherwise
     * @throws IllegalArgumentException if wrong generics count provided
     */
    public boolean isMoreSpecificGenerics(Type... arguments) {
        if (arguments.length != actualArguments.length) {
            throw new IllegalArgumentException(String.format(
                    "Wrong generics count provided <%s> in compare to current types <%s>",
                    TypeToStringUtils.toStringTypes(arguments, EmptyGenericsMap.getInstance()),
                    TypeToStringUtils.toStringTypes(actualArguments, EmptyGenericsMap.getInstance())));
        }
        for (int i = 0; i < arguments.length; i++) {
            // if at least one provided type is less specific then entire set is less specific
            if (TypeUtils.isMoreSpecific(actualArguments[i], arguments[i])) {
                return false;
            }
        }
        return true;
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
        // this type could represent class, but still be parameterized to hold instance
        res = !res && o instanceof Class && actualArguments.length == 0 && rawType == o;
        if (!res && o instanceof ParameterizedType) {
            final ParameterizedType that = (ParameterizedType) o;
            final Type[] thatActualArguments = that.getActualTypeArguments();
            final Type thatOwnerType = that.getOwnerType();
            final Type thatRawType = that.getRawType();

            res = rawType.equals(thatRawType)
                    && Arrays.equals(actualArguments, thatActualArguments)
                    && (ownerType != null ? ownerType.equals(thatOwnerType) : thatOwnerType == null);
        }
        return res;
    }

    @Override
    public int hashCode() {
        int result = rawType.hashCode();
        result = 31 * result + Arrays.hashCode(actualArguments);
        result = 31 * result + (ownerType != null ? ownerType.hashCode() : 0);
        return result;
    }


    private void analyze(final Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType ptype = (ParameterizedType) type;
            this.rawType = (Class<?>) ptype.getRawType();
            this.actualArguments = ptype.getActualTypeArguments();
            this.ownerType = ptype.getOwnerType();
        } else {
            rawType = GenericsUtils.resolveClass(type);
            ownerType = TypeUtils.getOuter(rawType);

            // possible generics resolved as upper bounds or taken from type (if ParameterizedType)
            final Map<String, Type> generics = GenericsResolutionUtils
                    .resolveGenerics(type, EmptyGenericsMap.getInstance());
            actualArguments = generics.values().toArray(new Type[0]);
        }
        completeType = actualArguments.length == 0;
    }
}

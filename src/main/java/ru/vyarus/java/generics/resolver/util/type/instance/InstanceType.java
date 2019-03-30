package ru.vyarus.java.generics.resolver.util.type.instance;

import java.lang.reflect.Type;

/**
 * Common interface for special types, resolved from object instance (or multiple instances).
 * Such types must hold original instance in order to be able to further improve typing.
 * <p>
 * Instance types are constructed using
 * {@link ru.vyarus.java.generics.resolver.util.TypeUtils#getInstanceType(Object...)}.
 * <p>
 * For example, if we have list of something, we can easily detect {@link java.util.List} type (using
 * {@code listObject.getClass()}), but in order to know what generic type this list has we need to know what list is
 * and how to extract items from it. So we create instance type for original list and holding list object inside it
 * so some custom logic (further analysis) could use this list instance in order to recover complete type.
 * <p>
 * Possible instance types:
 * <ul>
 * <li>{@link ParameterizedInstanceType} used to represent simple objects (with or without generics).
 * Note that this type is used even for pure {@link Class} because class type is final and can't be extended
 * to store instance (so in case of instance types for class arguments array would be empty)</li>
 * <li>{@link GenericArrayInstanceType} used for arrays. Note that component type would be also an instance
 * type - median for all items, contained in all arrays (if there are multiple arrays)</li>
 * <li>{@link WildcardInstanceType} used for wildcards. Wildcards commonly appear after searching for
 * median type. In this case only first type inside wildcard will be {@link ParameterizedInstanceType} and
 * could be further improved. Other types (interfaces) in wildcard remain simple types. This type is required
 * only for preserving all available type information.</li>
 * </ul>
 * <p>
 * Note that in case of multiple instances it it important to take into account all of them (especially in case of
 * containers. For example, suppose we have instance of {@code List<Set<?>>} then first instance type will contain
 * list instance. Next we extract list elements and create instance type from them (it would be {@code Set<?>} type.
 * And finally, to know the generic value for the set type we extracting type from all instances from all sets (because
 * one set could be misleading, e.g. {@code [int], [int, double]}: median type for first set is int and for second set
 * only Number and so it would be not correct to look only to the first set).
 * <p>
 * In order to avoid actual instance types structure knowledge, you can simply use {@link #getImprovableType()}
 * to properly resolve improvable type from inside any other instance type. This type could be used for
 * direct improvement. All container instanance types will also be improved as improvement modifies type instance
 * directly.
 * <p>
 * For simplicity, all instance types implements {@link Iterable} so you can use forEach to iterate on it's values.
 *
 * @author Vyacheslav Rusakov
 * @see ParameterizedInstanceType
 * @see GenericArrayInstanceType
 * @see WildcardInstanceType
 * @since 14.03.2019
 */
public interface InstanceType extends Iterable, Type {

    /**
     * @return original instance (first instance if multiple provided)
     */
    Object getInstance();

    /**
     * @return true if multiple instances contained, false if single instance
     */
    boolean hasMultipleInstances();

    /**
     * @return all contained instances (single element array in case of single instance)
     */
    Object[] getAllInstances();

    /**
     * E.g. {@link String} does not contain generics and so it's "complete" and
     * {@link java.util.List} contain generic T, which is unknown for instance (and so type is "incomplete").
     * <p>
     * In essence, complete type does not require further instance analysis (e.g. for list generic T could be guessed
     * from contained elements).
     * <p>
     * {@link ParameterizedInstanceType} become compete after
     * {@link ParameterizedInstanceType#improveAccuracy(Type...)} call (which means that all type information
     * was used form instance and there is no way to improve type further).
     *
     * @return true if type does not contain unknown generics, false otherwise
     */
    boolean isCompleteType();

    /**
     * Due to special wrapper types like {@link GenericArrayInstanceType} and {@link WildcardInstanceType}
     * which are pure containers actually (the first preserve original arrays and the later contains additional
     * interfaces for main type) it would be always required to know type structure to properly resolve actually
     * improvable types within them. To simplify usage, this method will return actual improvable type to work with
     * (it may be even type multiple levels deeper). Wrapper types will still benefit from improvement because improved
     * type remains the same instance (and so all types containing it being automatically improved).
     * <p>
     * {@link ParameterizedInstanceType} returns itself.
     *
     * @return instance type that could be actually improved or null if no improvable types contained
     */
    ParameterizedInstanceType getImprovableType();
}

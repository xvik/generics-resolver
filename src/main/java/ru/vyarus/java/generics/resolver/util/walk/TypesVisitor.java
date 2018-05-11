package ru.vyarus.java.generics.resolver.util.walk;

import java.lang.reflect.Type;

/**
 * Visitor interface for {@link TypesWalker}. Used to walk on two types synchronously.
 * Types hierarchy is assumed to be compatible. Use cases: types compatibility or deep comparision.
 *
 * @author Vyacheslav Rusakov
 * @see CompatibilityTypesVisitor
 * @since 11.05.2018
 */
public interface TypesVisitor {

    /**
     * Method is called for each resolved matching classes pair in types hierarchy. For example, walking on
     * {@code List<String>} and {@code ArrayList<Integer>} will lead to two calls with
     * {@code List, ArrayList} and then {@code String, Integer}.
     * <p>
     * Note that for array types, array classes will be provided too (to let you compare on root level). For example,
     * walking on {@code String[]} and {@code Integer[]} will lead to two calls with
     * {@code String[].class, Integer[].class} and then {@code String.class. Integer.class}.
     * <p>
     * If one of the types is {@code Object} then it will be the last step (no hierarchy incompatibility will be
     * called as object is compatible with everything - we just can't go further because of not enough information).
     *
     * @param one current class in first hierarchy
     * @param two class from second hierarchy, but from the same place (in type tree)
     * @return true to continue walking, false to stop
     */
    boolean next(Class<?> one, Class<?> two);

    /**
     * Called when synchronous walk is impossible.
     * <ul>
     * <li>One type is array and other is not</li>
     * <li>Types are not compatible (this means hierarchies doesn't match, even if classes doesn't use
     * generics and so will not be analyzed further)</li>
     * </ul>
     *
     * @param one current type from hierarchy one (not root type)
     * @param two current type from hierarchy two
     */
    void incompatibleHierarchy(Type one, Type two);
}

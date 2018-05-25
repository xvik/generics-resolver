package ru.vyarus.java.generics.resolver.util.walk;

import java.lang.reflect.Type;

/**
 * Visitor interface for {@link TypesWalker}. Used to walk on two types synchronously.
 * Types hierarchy is assumed to be compatible. Use cases: types compatibility or deep comparision.
 * <p>
 * Primitive types will never appear in visitor: wrapper types are used instead for simplicity (e.g. Integer for int).
 *
 * @author Vyacheslav Rusakov
 * @see CompatibilityTypesVisitor
 * @since 11.05.2018
 */
public interface TypesVisitor {

    /**
     * Method is called for each matching types pair in types hierarchy. For example, walking on
     * {@code List<String>} and {@code ArrayList<String>} will lead to two calls with
     * {@code List<String>, ArrayList<String>} and then {@code String, String}. Method may return false
     * to stop types traversing.
     * <p>
     * When types are not compatible then {@link #incompatibleHierarchy(Type, Type)} is called instead.
     * <p>
     * Passed types are always compatible on current level. For example {@code List<String> and List<Integer>}
     * are compatible on class level (assumed as {@code List and List}, but on the next level incompatible
     * type will be notified. Overall two visitor calls will be done: {@link #next(Type, Type)} for lists and
     * {@link #incompatibleHierarchy(Type, Type)} for list generics.
     * <p>
     * If one of the types is {@code Object} then it will be the last step (no hierarchy incompatibility will be
     * called as object is compatible with everything - we just can't go further because of not enough information).
     *
     * @param one current class in first hierarchy
     * @param two class from second hierarchy, but from the same place (in type tree)
     * @return true to continue walking, false to stop
     */
    boolean next(Type one, Type two);

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

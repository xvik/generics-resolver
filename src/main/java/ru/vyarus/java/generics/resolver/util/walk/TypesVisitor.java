package ru.vyarus.java.generics.resolver.util.walk;

import java.lang.reflect.Type;

/**
 * Visitor interface for {@link TypesWalker}. Used to walk on two types synchronously.
 * Types hierarchy is assumed to be compatible. Use cases: types compatibility or deep comparision.
 * <p>
 * Primitive types will never appear in visitor: wrapper types are used instead for simplicity (e.g. Integer for int).
 * <p>
 * If one or two types are inner types (inner non static, non interface classes) then first outer classes will be
 * compared. This may lead to immediate {@link #incompatibleHierarchy(Type, Type)} call with one type and one null
 * (if only one of types is inner type).
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
     * <p>
     * If types are inner types (inner non static classes), then first outer class hierarchy will be checked. E.g.
     * for types {@code Outer<S>.Inner<C, D>} and {@code Outer<T>.Inner<C, D>} first called
     * {@code next(Outer, Outer)}, then {@code next(S, T)} (if types compatible) or incompatible hierarchy. And only
     * after outer hierarchy check, actual {@code Inner} types would be compared.
     * <p>
     * Only one level of outer class is supported.
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
     * <p>
     * WARNING: one of two parameters may be null if one root type is outer type and another is not!
     *
     * @param one current type from hierarchy one (not root type)
     * @param two current type from hierarchy two
     */
    void incompatibleHierarchy(Type one, Type two);
}

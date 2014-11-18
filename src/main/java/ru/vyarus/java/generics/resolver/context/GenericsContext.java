package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.NoGenericException;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Context object wraps root type hierarchy generics information descriptor and provides utility methods for
 * actual types resolution.
 * <p>Usage: navigate to required type {@code context.type(MyClass.class)} and use utility methods to
 * get type's own generics or as helper for methods/fields introspection.</p>
 * <p>Every context object is immutable. Context doesn't hold actual types hierarchy (use reflection api in parallel
 * if you need hierarchy info). Navigation is allowed to any class (within original root class hierarchy) from any
 * child context. For convenience root class is also allowed (no matter the fact that it doesn't contain resolved
 * generics).</p>
 * <p>API operated mainly on types, because it's the only way to resolve recursive generics: e.g. when you have
 * {@code List<Integer>} then generic could be represented as {@code Integer} class, but if
 * {@code List<Collection<Integer>>} we can't represent generic as class ({@code Collection}),
 * because this makes actual collection generic not accessible)</p>
 * <p>Complete example: suppose we have {@code class A extends B<Integer>}.
 * {@code GenericsResolver.resolve(A.class).type(B.class).generic(0) == Integer.class}</p>
 *
 * @author Vyacheslav Rusakov
 * @since 17.11.2014
 */
// huge class size is OK, because it should be the only entry point for api
@SuppressWarnings("PMD.ExcessiveClassLength")
public class GenericsContext {
    private final GenericsInfo genericsInfo;
    private final Class<?> currentType;
    private final Map<String, Type> typeGenerics;

    public GenericsContext(final GenericsInfo genericsInfo, final Class<?> type) {
        this.genericsInfo = genericsInfo;
        this.currentType = type;
        // collection resolved for fail fast on wrong type
        typeGenerics = type == genericsInfo.getRootClass() ? Collections.<String, Type>emptyMap()
                : genericsInfo.getTypeGenerics(type);
    }

    /**
     * @return current context class
     */
    public Class<?> currentClass() {
        return currentType;
    }

    /**
     * {@code class A extends B<Object, C<Long>>}.
     * <pre>{@code type(B.class).genericTypes() == [Class<Object>, ParametrizedType] }</pre>
     *
     * @return current class generics types
     */
    public List<Type> genericTypes() {
        return new ArrayList<Type>(typeGenerics.values());
    }

    /**
     * {@code class A extends B<Object, C<Long>>}.
     * <pre>{@code type(B.class).generics() == [Class<Object>, Class<C>] }</pre>
     * Note that this way you loose second level generics info ({@code Long} from {@code C} class)
     *
     * @return current class generic classes
     */
    public List<Class<?>> generics() {
        final List<Class<?>> res = new ArrayList<Class<?>>();
        for (Type type : typeGenerics.values()) {
            res.add(resolveClass(type));
        }
        return res;
    }

    /**
     * {@code class A extends B<Object, C<Long>>}.
     * <pre>{@code type(B.class).genericsAsString() == ["Object", "C<Long>"] }</pre>
     * Note, that returned generics are completely resolved, e.g.
     * {@code A extends B<Object> }, where {@code B<T> extends C<T, List<T>>},
     * and when we call {@code type(C.class).genericsAsString() == ["Object", "List<Object>]}
     *
     * @return current generics string representation
     */
    public List<String> genericsAsString() {
        final List<String> res = new ArrayList<String>();
        for (Type type : typeGenerics.values()) {
            res.add(toStringType(type));
        }
        return res;
    }

    /**
     * {@code class A extends B<Object, C<Long>>}.
     * <pre>{@code type(B.class).genericType(1) == ParametrizedType }</pre>
     *
     * @param position generic position (from 0)
     * @return generic type
     * @throws IndexOutOfBoundsException for wrong index
     * @see #genericTypes() for details
     */
    public Type genericType(final int position) {
        return genericTypes().get(position);
    }

    /**
     * {@code class A extends B<Object, C<Long>>}.
     * <pre>{@code type(B.class).generic(1) == C.class }</pre>
     *
     * @param position generic position (from 0)
     * @return resolved generic class
     * @throws IndexOutOfBoundsException for wrong index
     * @see #resolveClass(java.lang.reflect.Type)
     */
    public Class<?> generic(final int position) {
        return resolveClass(genericTypes().get(position));
    }

    /**
     * {@code class A extends B<Object, C<Long>>}.
     * <pre>{@code type(B.class).genericAsString(1) == "C<Long>" }</pre>
     *
     * @param position generic position (from 0)
     * @return resolved generic string representation
     * @throws IndexOutOfBoundsException for wrong index
     * @see #toStringType(java.lang.reflect.Type)
     */
    public String genericAsString(final int position) {
        return toStringType(genericType(position));
    }

    /**
     * {@code class A extends B<Object, C<Long>>} and {@code class B<T, K>}.
     * <pre>{@code type(B.class).genericsMap() == ["T": Object.class, "K": ParametrizedType]}</pre>
     *
     * @return map of current generics (runtime mapping of generic name to actual type)
     */
    public Map<String, Type> genericsMap() {
        return new LinkedHashMap<String, Type>(typeGenerics);
    }

    /**
     * @return generics info object, which contains all information of root class hierarchy generics
     */
    public GenericsInfo getGenericsInfo() {
        return genericsInfo;
    }

    /**
     * Useful for introspection, to know exact return type of generified method.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     T doSmth();
     * }}</pre>
     * Resolving return type in type of root class:
     * {@code type(B.class).resolveReturnClass(B.class.getMethod("doSmth")) == Long.class}
     *
     * @param method method to analyze
     * @return resolved return class of method (generic resolved or, in case of simple class, returned as is)
     * @see #resolveClass(java.lang.reflect.Type)
     */
    public Class<?> resolveReturnClass(final Method method) {
        return GenericsUtils.getReturnClass(method, typeGenerics);
    }

    /**
     * Useful for introspection, to know exact parameter types.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     void doSmth(T a, Integer b);
     * }}</pre>
     * Resolving parameters in context of root class:
     * {@code type(B.class).resolveParameters(B.class.getMethod("doSmth", Object.class)) ==
     * [Long.class, Integer.class]}
     *
     * @param method method to analyze
     * @return resolved method parameters or empty list if method doesn't contain parameters
     */
    public List<Class<?>> resolveParameters(final Method method) {
        return GenericsUtils.getMethodParameters(method, typeGenerics);
    }

    /**
     * Useful for introspection to know exact class of type.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     T doSmth();
     * }}</pre>
     * Resolving class of return type:
     * {@code type(B.class).resolveClass(B.class.getMethod("doSmth").getGenericReturnType()) == Long.class}
     *
     * @param type type to resolve class
     * @return resolved type class
     */
    public Class<?> resolveClass(final Type type) {
        return GenericsUtils.resolveClass(type, typeGenerics);
    }

    /**
     * Useful for introspection, to know exact generic value.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     List<T> doSmth();
     * }}</pre>
     * Resolving parameters in context of root class:
     * {@code type(B.class).resolveGenericsOf(B.class.getMethod("doSmth").getGenericReturnType()) == [Long.class]}
     *
     * @param type type to resolve generics
     * @return resolved generic class
     * @throws NoGenericException if provided type does not contain generic (exception required to distinguish
     *                            {@code Object.class} generic value from class which doesn't support generic
     */
    public List<Class<?>> resolveGenericsOf(final Type type) throws NoGenericException {
        return GenericsUtils.resolveGenericsOf(type, typeGenerics);
    }

    /**
     * Shortcut for {@link #resolveGenericsOf(java.lang.reflect.Type)} useful for single generic types or
     * when just first generic required.
     *
     * @param type type to resolve generic
     * @return first resolved generic
     * @throws NoGenericException if provided type does not contain generic (exception required to distinguish
     *                            {@code Object.class} generic value from class which doesn't support generic
     */
    public Class<?> resolveGenericOf(final Type type) throws NoGenericException {
        return resolveGenericsOf(type).get(0);
    }

    /**
     * Useful for reporting or maybe logging. Resolves all generics and compose resulted type as string.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     List<T> doSmth();
     * }}</pre>
     * Resolving parameters in type of root class:
     * {@code type(B.class).toStringType(B.class.getMethod("doSmth").getGenericReturnType()) == "List<Long>"}
     *
     * @param type to to get string of
     * @return string representation for resolved type
     */
    public String toStringType(final Type type) {
        return TypeToStringUtils.toStringType(type, typeGenerics);
    }

    /**
     * Navigates current context to specific type in class hierarchy.
     *
     * @param type class to navigate to
     * @return new context instance specific to requested class
     * @throws IllegalArgumentException if requested class is not present in root class hierarchy
     */
    public GenericsContext type(final Class<?> type) {
        return new GenericsContext(genericsInfo, type);
    }
}

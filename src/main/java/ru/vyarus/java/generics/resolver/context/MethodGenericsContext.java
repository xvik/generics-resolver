package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.NoGenericException;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Method generics context. Additional context is required because method could contain it's own generics, for example:
 * <pre>{@code class A {
 *     <T> T method();
 * }}</pre>
 * Such generics are not known in type and as a result any operation on type with such generic will lead to
 * unknown generic.
 * <p>Also, context contains special methods for parameters and return type analysis.</p>
 *
 * @author Vyacheslav Rusakov
 * @since 26.06.2015
 */
@SuppressWarnings("PMD.PreserveStackTrace")
public class MethodGenericsContext extends GenericsContext {

    private final Method method;
    private Map<String, Type> methodGenerics;
    private Map<String, Type> allGenerics;

    public MethodGenericsContext(final GenericsInfo genericsInfo, final Class<?> type, final Method method) {
        super(genericsInfo, type);
        final Class<?> declaringType = method.getDeclaringClass();
        if (!declaringType.equals(type)) {
            throw new IllegalArgumentException(String.format(
                    "Method '%s' should be resolved on type %s and not %s",
                    method.getName(), declaringType.getSimpleName(), type.getSimpleName()));
        }
        this.method = method;
        initGenerics();
    }

    /**
     * @return current context method
     */
    public Method currentMethod() {
        return method;
    }

    /**
     * {@code <T extends Serializable> T method();}.
     * <pre>{@code context.method(B.getMethod('method')).methodGenericTypes() == [Class<Serializable>]}</pre>
     *
     * @return current method generics types
     */
    public List<Type> methodGenericTypes() {
        return new ArrayList<Type>(methodGenerics.values());
    }

    /**
     * <pre>{@code class A<E> {
     *     <T, K extends E> K method(T arg);
     * }
     * class B extends A<Serializable>}</pre>
     * <pre>{@code context.method(A.getMethod("method", Object.class)).methodGenericsMap() ==
     *          ["T": Object.class, "K": Serializable.class]}</pre>
     * For method generics it's impossible to know actual type (available only in time of method call),
     * so generics resolved as lower bound.
     *
     * @return map of current method generics (runtime mapping of generic name to actual type)
     */
    public Map<String, Type> methodGenericsMap() {
        return new LinkedHashMap<String, Type>(methodGenerics);
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
     * @return resolved return class of method (generic resolved or, in case of simple class, returned as is)
     * @see #resolveClass(java.lang.reflect.Type)
     */
    public Class<?> resolveReturnClass() {
        return GenericsUtils.getReturnClass(method, contextGenerics());
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
     * @return resolved method parameters or empty list if method doesn't contain parameters
     */
    public List<Class<?>> resolveParameters() {
        return GenericsUtils.getMethodParameters(method, contextGenerics());
    }

    /**
     * Useful for introspection, to know exact generic value.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     List<T> doSmth();
     * }}</pre>
     * Resolving parameters in context of root class:
     * {@code type(B.class).method(B.class.getMethod("doSmth")).resolveReturnTypeGenerics() == [Long.class]}
     *
     * @return resolved generic class
     * @throws NoGenericException if provided type does not contain generic (exception required to distinguish
     *                            {@code Object.class} generic value from class which doesn't support generic
     */
    public List<Class<?>> resolveReturnTypeGenerics() throws NoGenericException {
        return GenericsUtils.resolveGenericsOf(method.getGenericReturnType(), contextGenerics());
    }

    /**
     * Shortcut for {@link #resolveReturnTypeGenerics()} useful for single generic types or
     * when just first generic required.
     *
     * @return first resolved generic
     * @throws NoGenericException if provided type does not contain generic (exception required to distinguish
     *                            {@code Object.class} generic value from class which doesn't support generic
     */
    public Class<?> resolveReturnTypeGeneric() throws NoGenericException {
        return resolveReturnTypeGenerics().get(0);
    }

    @Override
    protected Map<String, Type> contextGenerics() {
        return allGenerics;
    }

    private void initGenerics() {
        final TypeVariable<Method>[] methodGenerics = method.getTypeParameters();
        final boolean hasMethodGenerics = methodGenerics.length > 0;
        this.methodGenerics = hasMethodGenerics
                ? new LinkedHashMap<String, Type>() : Collections.<String, Type>emptyMap();
        // important to fill it in time of resolution because method generics could be dependant
        this.allGenerics = hasMethodGenerics
                ? new LinkedHashMap<String, Type>(typeGenerics) : typeGenerics;
        for (TypeVariable<Method> generic : methodGenerics) {
            final Class<?> value = resolveClass(generic.getBounds()[0]);
            this.methodGenerics.put(generic.getName(), value);
            this.allGenerics.put(generic.getName(), value);
        }
    }
}

package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Method generics context. Additional context is required because method could contain it's own generics, for example:
 * <pre>{@code class A {
 *     <T> T method();
 * }}</pre>
 * Such generics are not known in type and, as a result, any operation on type with such generic will lead to
 * unknown generic exception.
 * <p>
 * Also, context contains special methods for parameters and return type analysis.
 *
 * @author Vyacheslav Rusakov
 * @since 26.06.2015
 */
@SuppressWarnings("PMD.PreserveStackTrace")
public class MethodGenericsContext extends TypeGenericsContext {

    private final Method meth;
    private Map<String, Type> methodGenerics;
    private Map<String, Type> allGenerics;

    public MethodGenericsContext(final GenericsInfo genericsInfo, final Method method, final GenericsContext root) {
        super(genericsInfo, method.getDeclaringClass(), root);
        this.meth = method;
        initGenerics();
    }

    /**
     * @return current context method
     */
    public Method currentMethod() {
        return meth;
    }

    /**
     * {@code <T extends Serializable> T method();}.
     * <pre>{@code context.method(A.getMethod('method')).methodGenericTypes() == [Class<Serializable>]}</pre>
     *
     * @return current method generics types
     */
    public List<Type> methodGenericTypes() {
        return methodGenerics.isEmpty()
                ? Collections.<Type>emptyList() : new ArrayList<Type>(methodGenerics.values());
    }

    /**
     * <pre>{@code class A<E> {
     *     <T, K extends E> K method(T arg);
     * }
     * class B extends A<Serializable> {}
     * }</pre>
     * <pre>{@code context.method(A.class.getMethod("method", Object.class)).methodGenericsMap() ==
     *          ["T": Object.class, "K": Serializable.class]}</pre>
     * For method generics it's impossible to know actual type (available only in time of method call),
     * so generics resolved as upper bound.
     *
     * @return map of current method generics (runtime mapping of generic name to actual type)
     */
    public Map<String, Type> methodGenericsMap() {
        return methodGenerics.isEmpty()
                ? Collections.<String, Type>emptyMap() : new LinkedHashMap<String, Type>(methodGenerics);
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
        return GenericsUtils.getReturnClass(meth, contextGenerics());
    }

    /**
     * Useful for introspection, to know exact parameter types.
     * <pre>{@code class A extends B<Long> {}
     * class B<T> {
     *     void doSmth(T a, Integer b);
     * }}</pre>
     * Resolving parameters in context of root class:
     * {@code method(B.class.getMethod("doSmth", Object.class, Integer.class)).resolveParameters() ==
     * [Long.class, Integer.class]}
     *
     * @return resolved method parameters or empty list if method doesn't contain parameters
     * @see #resolveParametersTypes()
     */
    public List<Class<?>> resolveParameters() {
        return GenericsUtils.resolveClasses(meth.getGenericParameterTypes(), contextGenerics());
    }

    /**
     * Returns parameter types with resolved generic variables.
     * <pre>{@code class A extends B<Long> {}
     * class B<T>{
     *     void doSmth(List<T> a);
     * }}</pre>
     * Resolving parameters types in context of root class:
     * {@code method(B.class.getMethod("doSmth", List.class)).resolveParametersTypes() == [List<Long>]}
     *
     * @return resolved method parameters types or empty list if method doesn't contain parameters
     * @see #resolveParameters()
     */
    public List<Type> resolveParametersTypes() {
        return Arrays.asList(GenericsUtils.resolveTypeVariables(meth.getGenericParameterTypes(), contextGenerics()));
    }

    /**
     * @param pos parameter position (form 0)
     * @return parameter type with resolved generic variables
     * @throws IllegalArgumentException if parameter index is incorrect
     */
    public Type resolveParameterType(final int pos) {
        checkParameter(pos);
        return resolveType(meth.getGenericParameterTypes()[pos]);
    }

    /**
     * Create generics context for parameter type (with correctly resolved root generics). "Drill down".
     * <pre>{@code class A<T> {
     *    void doSmth(B<T>);
     * }
     * class C extends A<String> {}}</pre>
     * Build generics context for parameter type (to continue analyzing parameter type fields):
     * {@code (context of C).method(A.class.getMethod("getSmth", B.class)).parameterType(0)
     * == generics context of B<String>}
     * <p>
     * Note that, in contrast to direct resolution {@code GenericsResolver.resolve(B.class)}, actual root generic
     * would be counted for hierarchy resolution.
     *
     * @param pos parameter position (from 0)
     * @return generics context of parameter type
     * @throws IllegalArgumentException if parameter index is incorrect
     * @see #inlyingType(Type)
     */
    public TypeGenericsContext parameterType(final int pos) {
        checkParameter(pos);
        return inlyingType(meth.getGenericParameterTypes()[pos]);
    }

    /**
     * Create generics context for actual class, passed into parameter type (assuming you have access to that instance
     * or know exact type). Context will contain correct generics for known declaration type (middle type in
     * target type hierarchy). This is useful when analyzing object instance (introspecting actual object).
     * <p>
     * Other than target type, method is the same as {@link #parameterType(int)}.
     *
     * @param pos    parameter position (from 0)
     * @param asType required target type to build generics context for (must include declared type as base class)
     * @return generics context of requested type with known parameter generics
     * @throws IllegalArgumentException if parameter index is incorrect
     * @see #inlyingTypeAs(Type, Class)
     */
    public TypeGenericsContext parameterTypeAs(final int pos, final Class<?> asType) {
        checkParameter(pos);
        return inlyingTypeAs(meth.getGenericParameterTypes()[pos], asType);
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
     * @return resolved generic classes or empty list if type does not use generics
     */
    public List<Class<?>> resolveReturnTypeGenerics() {
        return GenericsUtils.resolveGenericsOf(meth.getGenericReturnType(), contextGenerics());
    }

    /**
     * Shortcut for {@link #resolveReturnTypeGenerics()} useful for single generic types or
     * when just first generic required.
     *
     * @return first resolved generic or null if type doesn't use generics
     */
    public Class<?> resolveReturnTypeGeneric() {
        final List<Class<?>> res = resolveReturnTypeGenerics();
        return res.isEmpty() ? null : res.get(0);
    }

    /**
     * <pre>{@code class B extends A<Long> {}
     * class A<T> {
     *      List<T> get();
     * }}</pre>.
     * {@code (context of B).method(A.class.getMethod("get")).resolveReturnType() == List<Long>}
     *
     * @return method return type with resolved generic variables
     */
    public Type resolveReturnType() {
        return resolveType(meth.getGenericReturnType());
    }

    /**
     * Create generics context for return type (with correctly resolved root generics). "Drill down".
     * <pre>{@code class A<T> {
     *    B<T> doSmth();
     * }
     * class C extends A<String> {}}</pre>
     * Build generics context for returning type (to continue analyzing return type fields):
     * {@code type(A.class).method(A.class.getMethod("getSmth")).returnType() == generics context of B<String>}
     * <p>
     * Note that, in contrast to direct resolution {@code GenericsResolver.resolve(B.class)}, actual root generic
     * would be counted for hierarchy resolution.
     *
     * @return generics context of return type
     * @see #inlyingType(Type)
     */
    public TypeGenericsContext returnType() {
        return inlyingType(meth.getGenericReturnType());
    }

    /**
     * Create generics context for actual class, returned form method (assuming you have access to that instance
     * or know exact type). Context will contain correct generics for known declaration type (middle type in target
     * type hierarchy). This is useful when analyzing object instance (introspecting actual object).
     * <p>
     * Other than target type, method is the same as {@link #returnType()}.
     *
     * @param asType required target type to build generics context for (must include declared type as base class)
     * @return generics context of requested type with known return type generics
     * @see #inlyingTypeAs(Type, Class)
     */
    public TypeGenericsContext returnTypeAs(final Class<?> asType) {
        return inlyingTypeAs(meth.getGenericReturnType(), asType);
    }

    /**
     * @return method declaration string with actual types instead of generic variables
     */
    public String toStringMethod() {
        return TypeToStringUtils.toStringMethod(meth, contextGenerics());
    }

    @Override
    public String toString() {
        return genericsInfo.toStringHierarchy(new MethodContextWriter());
    }

    @Override
    public GenericDeclarationScope getGenericsScope() {
        return GenericDeclarationScope.METHOD;
    }

    @Override
    public GenericDeclaration getGenericsSource() {
        return currentMethod();
    }

    @Override
    public MethodGenericsContext method(final Method method) {
        // optimization
        return method == currentMethod() ? this : super.method(method);
    }

    @Override
    protected Map<String, Type> contextGenerics() {
        return allGenerics;
    }

    private void initGenerics() {
        final TypeVariable<Method>[] methodGenerics = meth.getTypeParameters();
        final boolean hasMethodGenerics = methodGenerics.length > 0;
        this.methodGenerics = hasMethodGenerics
                ? new LinkedHashMap<String, Type>() : Collections.<String, Type>emptyMap();
        // important to fill it in time of resolution because method generics could be dependant
        this.allGenerics = hasMethodGenerics
                ? new LinkedHashMap<String, Type>(allTypeGenerics) : allTypeGenerics;
        for (TypeVariable<Method> generic : methodGenerics) {
            final Class<?> value = resolveClass(generic.getBounds()[0]);
            this.methodGenerics.put(generic.getName(), value);
            this.allGenerics.put(generic.getName(), value);

            // method generics may override class or owner class generics, but
            // genericsMap() and ownerGenericsMap() should return the same in all cases for consistency
        }
    }

    private void checkParameter(final int pos) {
        final Type[] genericParams = meth.getGenericParameterTypes();
        if (pos < 0 || pos >= genericParams.length) {
            throw new IllegalArgumentException(String.format(
                    "Can't request parameter %s of method '%s' (%s) because it have only %s parameters",
                    pos, toStringMethod(), currentClass().getSimpleName(), genericParams.length));
        }
    }

    /**
     * Hierarchy writer with current method identification.
     */
    class MethodContextWriter extends RootContextAwareTypeWriter {
        @Override
        public String write(final Class<?> type,
                            final Map<String, Type> generics,
                            final Class<?> owner,
                            final Map<String, Type> ownerGenerics,
                            final String shift) {
            String method = "";
            if (type == currentType) {
                method = String.format("%n%s%s%s%s",
                        shift,
                        GenericsInfo.SHIFT_MARKER,
                        toStringMethod(),
                        CURRENT_POSITION_MARKER);
            }
            return super.write(type, generics, owner, ownerGenerics, shift) + method;
        }
    }
}

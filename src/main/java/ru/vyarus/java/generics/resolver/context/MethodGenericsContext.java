package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

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
public class MethodGenericsContext extends TypeGenericsContext {

    private static final String SPLIT = ", ";

    private final Method method;
    private Map<String, Type> methodGenerics;
    private Map<String, Type> allGenerics;

    public MethodGenericsContext(final GenericsInfo genericsInfo, final Method method) {
        super(genericsInfo, method.getDeclaringClass());
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
     * @param pos parameter position (form 0)
     * @return parameter type with resolved generic variables
     * @throws IllegalArgumentException if parameter index is incorrect
     */
    public Type resolveParameterType(final int pos) {
        checkParameter(pos);
        return resolveType(method.getGenericParameterTypes()[pos]);
    }

    /**
     * Create generics context for parameter type (with correctly resolved root generics). "Drill down".
     * <pre>{@code class A<T> {
     *    void doSmth(B<T>);
     * }
     * class C extends A<String> {}}</pre>
     * Build generics context for parameter type (to continue analyzing parameter type fields):
     * {@code type(A.class).method(A.class.getMethod("getSmth", B.class)).parameterType(0)
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
        return inlyingType(method.getGenericParameterTypes()[pos]);
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
     * @see #inlyingTypeAs(Type, Class)
     */
    public TypeGenericsContext parameterTypeAs(final int pos, final Class<?> asType) {
        checkParameter(pos);
        return inlyingTypeAs(method.getGenericParameterTypes()[pos], asType);
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
        return GenericsUtils.resolveGenericsOf(method.getGenericReturnType(), contextGenerics());
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
     * @return method return type with resolved generic variables
     */
    public Type resolveReturnType() {
        return resolveType(method.getGenericReturnType());
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
        return inlyingType(method.getGenericReturnType());
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
        return inlyingTypeAs(method.getGenericReturnType(), asType);
    }

    /**
     * @return method declaration string with actual generics
     */
    public String toStringMethod() {
        return String.format("%s %s%s",
                TypeToStringUtils.toStringType(resolveReturnType(), contextGenerics()),
                method.getName(),
                toStringMethodParameters());
    }

    @Override
    public String toString() {
        return genericsInfo.toStringHierarchy(new MethodContextWriter());
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
                ? new LinkedHashMap<String, Type>(allTypeGenerics) : allTypeGenerics;
        for (TypeVariable<Method> generic : methodGenerics) {
            final Class<?> value = resolveClass(generic.getBounds()[0]);
            this.methodGenerics.put(generic.getName(), value);
            this.allGenerics.put(generic.getName(), value);
        }
    }

    private void checkParameter(final int pos) {
        final Type[] genericParams = method.getGenericParameterTypes();
        if (pos < 0 || pos >= genericParams.length) {
            throw new IllegalArgumentException(String.format(
                    "Can't request parameter %s of method '%s' (%s) because it have only %s parameters",
                    pos, method.getName(), currentClass().getSimpleName(), genericParams.length));
        }
    }

    private String toStringMethodParameters() {
        final int count = method.getParameterTypes().length;
        final StringBuilder res = new StringBuilder(count * 10 + 2)
                .append("(");
        if (count > 0) {
            boolean first = true;
            for (Type type : method.getGenericParameterTypes()) {
                if (!first) {
                    res.append(SPLIT);
                }
                res.append(TypeToStringUtils.toStringType(type, allGenerics));
                first = false;
            }
        }
        return res.append(")").toString();
    }

    /**
     * Hierarchy writer with current method identification.
     */
    class MethodContextWriter extends GenericsInfo.DefaultTypeWriter {
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

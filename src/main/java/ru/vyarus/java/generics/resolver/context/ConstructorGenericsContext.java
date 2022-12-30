package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Constructor generics context. Additional context is required because constructor could contain it's own generics,
 * for example:
 * <pre>{@code class A {
 *     <T> A(T arg);
 * }}</pre>
 * Such generics are not known in type and, as a result, any operation on type with such generic will lead to
 * unknown generic exception.
 * <p>
 * Also, context contains special methods for parameters analysis.
 *
 * @author Vyacheslav Rusakov
 * @since 29.05.2018
 */
public class ConstructorGenericsContext extends GenericsContext {

    private final Constructor ctor;
    private Map<String, Type> constructorGenerics;
    private Map<String, Type> allGenerics;

    public ConstructorGenericsContext(final GenericsInfo genericsInfo,
                                      final Constructor constructor,
                                      final GenericsContext root) {
        super(genericsInfo, constructor.getDeclaringClass(), root);
        this.ctor = constructor;
        initGenerics();
    }

    /**
     * @return current context constructor
     */
    public Constructor currentConstructor() {
        return ctor;
    }

    /**
     * {@code <T extends Serializable> Some(T arg);}.
     * <pre>{@code context.constructor(Some.class.getConstructor(Object.class)).constructorGenericTypes() ==
     * [Class<Serializable>]}</pre>
     *
     * @return current constructor generics types
     */
    public List<Type> constructorGenericTypes() {
        return constructorGenerics.isEmpty()
                ? Collections.<Type>emptyList() : new ArrayList<Type>(constructorGenerics.values());
    }

    /**
     * <pre>{@code class A<E>{
     *     <T, K extends E> A(T arg1, K arg2);
     * }
     * class B extends A<Serializable> {}
     * }</pre>
     * <pre>{@code context.constructor(A.class.getConstructor(Object.class, Object.class)).constructorGenericsMap() ==
     *      ["T": Object.class, "K": Serializable.class]}</pre>
     * For constructor generics it's impossible to know actual type (available only in time of constructor call),
     * so generics resolved as upper bound.
     *
     * @return map of current constructor generics (runtime mapping of generic name to actual type)
     */
    public Map<String, Type> constructorGenericsMap() {
        return constructorGenerics.isEmpty()
                ? Collections.<String, Type>emptyMap() : new LinkedHashMap<String, Type>(constructorGenerics);
    }

    /**
     * Useful for introspection, to know exact parameter types.
     * <pre>{@code class A extends B<Long> {}
     * class B<T> {
     *     B(T a, Integer b);
     * }}</pre>
     * Resolving parameters in context of root class:
     * {@code constructor(B.class.getConstructor(Object.class)).resolveParameters() ==
     * [Long.class, Integer.class]}
     * <p>
     * Note: may return primitives because it might be important to differentiate actual value.
     * Use {@link ru.vyarus.java.generics.resolver.util.TypeUtils#wrapPrimitive(Class)} to box possible primitive,
     * if required.
     *
     * @return resolved constructor parameters or empty list if constructor doesn't contain parameters
     * @see #resolveParametersTypes()
     */
    public List<Class<?>> resolveParameters() {
        return GenericsUtils.resolveClasses(ctor.getGenericParameterTypes(), contextGenerics());
    }

    /**
     * Returns parameter types with resolved generic variables.
     * <pre>{@code class A extends B<Long> {}
     * class B<T>{
     *     B(List<T> arg);
     * }}</pre>
     * Resolving parameters types in context of root class:
     * {@code constructor(B.class.getConstructor(List.class)).resolveParametersTypes() == [List<Long>]}
     * <p>
     * Note: may return primitive because it might be important to differentiate actual value.
     * Use {@link ru.vyarus.java.generics.resolver.util.TypeUtils#wrapPrimitive(Class)} to box possible primitive,
     * if required.
     *
     * @return resolved constructor parameters types or empty list if constructor doesn't contain parameters
     * @see #resolveParameters()
     */
    public List<Type> resolveParametersTypes() {
        return Arrays.asList(GenericsUtils.resolveTypeVariables(
                ctor.getGenericParameterTypes(), contextGenerics()));
    }

    /**
     * Note: may return primitive because it might be important to differentiate actual value.
     * Use {@link ru.vyarus.java.generics.resolver.util.TypeUtils#wrapPrimitive(Class)} to box possible primitive,
     * if required.
     *
     * @param pos parameter position (form 0)
     * @return parameter type with resolved generic variables
     * @throws IllegalArgumentException if parameter index is incorrect
     */
    public Type resolveParameterType(final int pos) {
        checkParameter(pos);
        return resolveType(ctor.getGenericParameterTypes()[pos]);
    }

    /**
     * Create generics context for parameter type (with correctly resolved root generics). "Drill down".
     * <pre>{@code class A<T> {
     *    A(B<T> arg);
     * }
     * class C extends A<String> {}}</pre>
     * Build generics context for parameter type (to continue analyzing parameter type fields):
     * {@code (context of C).constructor(A.class.getConstructor(B.class)).parameterType(0)
     * == generics context of B<String>}
     * <p>
     * Note that, in contrast to direct resolution {@code GenericsResolver.resolve(B.class)}, actual root generic
     * would be counted for hierarchy resolution.
     * <p>
     * For primitive constructor parameters wrapper class would be used (for example. if target parameter is
     * {@code int} then returned context type would be {@link Integer}). It is not hard to detect primitives
     * manually when required (it may be only directly declared primitive argument because it is impossible to
     * declare primitive with generic variable).
     *
     * @param pos parameter position (from 0)
     * @return generics context of parameter type
     * @throws IllegalArgumentException if parameter index is incorrect
     * @see #inlyingType(Type)
     */
    public GenericsContext parameterType(final int pos) {
        checkParameter(pos);
        return inlyingType(ctor.getGenericParameterTypes()[pos]);
    }

    /**
     * Create generics context for actual class, passed into parameter type (assuming you have access to that instance
     * or know exact type). Context will contain correct generics for known declaration type (middle type in
     * target type hierarchy). This is useful when analyzing object instance (introspecting actual object).
     * <p>
     * Other than target type, method is the same as {@link #parameterType(int)}.
     * <p>
     * For primitive constructor parameters wrapper class would be used (for example. if target parameter is
     * {@code int} then returned context type would be {@link Integer}). It is not hard to detect primitives
     * manually when required (it may be only directly declared primitive argument because it is impossible to
     * declare primitive with generic variable).
     *
     * @param pos    parameter position (from 0)
     * @param asType required target type to build generics context for (must include declared type as base class)
     * @return generics context of requested type with known parameter generics
     * @throws IllegalArgumentException if parameter index is incorrect
     * @see #inlyingTypeAs(Type, Class)
     */
    public GenericsContext parameterTypeAs(final int pos, final Class<?> asType) {
        checkParameter(pos);
        return inlyingTypeAs(ctor.getGenericParameterTypes()[pos], asType);
    }


    /**
     * @return constructor declaration string with actual types instead of generic variables
     */
    public String toStringConstructor() {
        return TypeToStringUtils.toStringConstructor(ctor, contextGenerics());
    }

    @Override
    public String toString() {
        return genericsInfo.toStringHierarchy(new ConstructorContextWriter());
    }

    @Override
    public GenericDeclarationScope getGenericsScope() {
        return GenericDeclarationScope.CONSTRUCTOR;
    }

    @Override
    public GenericDeclaration getGenericsSource() {
        return currentConstructor();
    }

    @Override
    public ConstructorGenericsContext constructor(final Constructor constructor) {
        // optimization
        return constructor.equals(currentConstructor()) ? this : super.constructor(constructor);
    }

    @Override
    protected Map<String, Type> contextGenerics() {
        return allGenerics;
    }

    @SuppressWarnings("unchecked")
    private void initGenerics() {
        final TypeVariable<Constructor>[] constrGenerics = ctor.getTypeParameters();
        final boolean hasConstrGenerics = constrGenerics.length > 0;
        this.constructorGenerics = hasConstrGenerics
                ? GenericsResolutionUtils.resolveDirectRawGenerics(ctor, allTypeGenerics)
                : Collections.<String, Type>emptyMap();
        // important to fill it in time of resolution because constructor generics could be dependant
        if (hasConstrGenerics) {
            // method generics may override class or owner class generics, but
            // genericsMap() and ownerGenericsMap() should return the same in all cases for consistency
            this.allGenerics = new LinkedHashMap<>(allTypeGenerics);
            this.allGenerics.putAll(this.constructorGenerics);
        } else {
            this.allGenerics = allTypeGenerics;
        }
    }

    private void checkParameter(final int pos) {
        final Type[] genericParams = ctor.getGenericParameterTypes();
        if (pos < 0 || pos >= genericParams.length) {
            throw new IllegalArgumentException(String.format(
                    "Can't request parameter %s of constructor '%s' because it has only %s parameters",
                    pos, toStringConstructor(), genericParams.length));
        }
    }

    /**
     * Hierarchy writer with current constructor identification.
     */
    class ConstructorContextWriter extends RootContextAwareTypeWriter {
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
                        toStringConstructor(),
                        CURRENT_POSITION_MARKER);
            }
            return super.write(type, generics, owner, ownerGenerics, shift) + method;
        }
    }
}

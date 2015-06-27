package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.NoGenericException;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.UnknownGenericException;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Context object wraps root type hierarchy generics information descriptor and provides utility methods for
 * actual types resolution. Currently there are two types of contexts: type context (class) and method context.
 * <p>Usage: navigate to required type {@code context.type(MyClass.class)} and use utility methods to
 * get type's own generics or as helper for methods/fields introspection.
 * To navigate to method context use {@code context.method(method)}.</p>
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
@SuppressWarnings({"PMD.ExcessiveClassLength", "PMD.PreserveStackTrace"})
public abstract class GenericsContext {
    protected final GenericsInfo genericsInfo;
    protected final Class<?> currentType;
    protected final Map<String, Type> typeGenerics;

    public GenericsContext(final GenericsInfo genericsInfo, final Class<?> type) {
        this.genericsInfo = genericsInfo;
        this.currentType = type;
        // collection resolved for fail fast on wrong type
        typeGenerics = genericsInfo.getTypeGenerics(type);
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
        for (Type type : contextGenerics().values()) {
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
     * {@code class B<T, K>}.
     * <pre>{@code class A extends B<Object, C<Long>>
     * type(B.class).genericType("K") == ParametrizedType }</pre>
     *
     * @param genericName generic position (from 0)
     * @return generic type
     * @throws java.lang.IllegalArgumentException for wrong generic name
     * @see #genericTypes() for details
     */
    public Type genericType(final String genericName) {
        return contextGenerics().get(checkGenericName(genericName));
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
     * {@code class B<T, K>}.
     * <pre>{@code class A extends B<Object, C<Long>>
     * type(B.class).generic("K") == C.class }</pre>
     *
     * @param genericName generic name
     * @return resolved generic class
     * @throws java.lang.IllegalArgumentException for wrong generic name
     * @see #resolveClass(java.lang.reflect.Type)
     */
    public Class<?> generic(final String genericName) {
        return resolveClass(contextGenerics().get(checkGenericName(genericName)));
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
     * {@code class B<T, K>}.
     * <pre>{@code class A extends B<Object, C<Long>>
     * type(B.class).genericAsString("K") == "C<Long>" }</pre>
     *
     * @param genericName generic name
     * @return resolved generic string representation
     * @throws java.lang.IllegalArgumentException for wrong generic name
     * @see #toStringType(java.lang.reflect.Type)
     */
    public String genericAsString(final String genericName) {
        return toStringType(contextGenerics().get(checkGenericName(genericName)));
    }

    /**
     * {@code class A extends B<Object, C<Long>>} and {@code class B<T, K>}.
     * <pre>{@code type(B.class).genericsMap() == ["T": Object.class, "K": ParametrizedType]}</pre>
     *
     * @return map of current generics (runtime mapping of generic name to actual type)
     */
    public Map<String, Type> genericsMap() {
        return new LinkedHashMap<String, Type>(contextGenerics());
    }

    /**
     * @return generics info object, which contains all information of root class hierarchy generics
     */
    public GenericsInfo getGenericsInfo() {
        return genericsInfo;
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
        try {
            return GenericsUtils.resolveClass(type, contextGenerics());
        } catch (UnknownGenericException e) {
            throw e.rethrowWithType(currentType);
        }
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
        try {
            return GenericsUtils.resolveGenericsOf(type, contextGenerics());
        } catch (UnknownGenericException e) {
            throw e.rethrowWithType(currentType);
        }
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
        try {
            return resolveGenericsOf(type).get(0);
        } catch (UnknownGenericException e) {
            throw e.rethrowWithType(currentType);
        }
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
        try {
            return TypeToStringUtils.toStringType(type, contextGenerics());
        } catch (UnknownGenericException e) {
            throw e.rethrowWithType(currentType);
        }
    }

    /**
     * Navigates current context to specific type in class hierarchy.
     *
     * @param type class to navigate to
     * @return new context instance specific to requested class
     * @throws IllegalArgumentException if requested class is not present in root class hierarchy
     */
    public TypeGenericsContext type(final Class<?> type) {
        return new TypeGenericsContext(genericsInfo, type);
    }

    /**
     * Navigates current context to specific method (type context is switched(!) to method declaring class).
     * It is required because method could contain it's own generics.
     * For example, {@code <T> void myMethod(T arg);}.
     * <p>Use context to work with method parameters, return type or resolving types inside method.</p>
     *
     * @param method method in current class to navigate to
     * @return new context instance specific to requested method
     * @throws IllegalArgumentException if requested method is not present in current class hierarchy
     */
    public MethodGenericsContext method(final Method method) {
        return new MethodGenericsContext(genericsInfo, method.getDeclaringClass(), method);
    }

    /**
     * @return resolved generics mapping for current context
     */
    protected abstract Map<String, Type> contextGenerics();

    private String checkGenericName(final String genericName) {
        if (!contextGenerics().containsKey(genericName)) {
            throw new UnknownGenericException(currentType, genericName);
        }
        return genericName;
    }
}

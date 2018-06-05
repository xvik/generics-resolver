package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.error.UnknownGenericException;
import ru.vyarus.java.generics.resolver.error.WrongGenericsContextException;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.*;
import java.util.*;

/**
 * Context object wraps root type hierarchy generics information descriptor and provides utility methods for
 * actual types resolution. This base class contains all type resolution logic (commonly used), whereas type specific
 * methods are added in {@link GenericsContext}, method context {@link MethodGenericsContext} or
 * constructor context {@link ConstructorGenericsContext}.
 * <p>
 * Note that all possible contexts extends from {@link GenericsContext}. {@link AbstractGenericsContext}
 * exists only to extract all core generics operations into separate class.
 * <p>
 * Usage: navigate to required type {@code context.type(MyClass.class)} and use utility methods to
 * get type's own generics or as helper for methods/fields introspection. To navigate to method context use
 * {@code context.method(method)}.
 * <p>
 * Every context object is immutable. Navigation is allowed to any class (within original root class hierarchy) from
 * any child context.
 * <p>
 * API operated mainly on types, because it's the only way to resolve recursive generics: e.g. when you have
 * {@code List<Integer>} then generic could be represented as {@code Integer} class, but if
 * {@code List<Collection<Integer>>} we can't represent generic as class ({@code Collection}),
 * because this makes actual collection generic not accessible)
 * <p>
 * Complete example: suppose we have {@code class A extends B<Integer>}.
 * {@code GenericsResolver.resolve(A.class).type(B.class).generic(0) == Integer.class}
 * <p>
 * It may be required to build context for some type, inside resolved hierarchy (e.g. for field). To do it
 * use {@link #inlyingType(Type)} or {@link #inlyingTypeAs(Type, Class)}. Essentially, this is teh same as
 * building usual context, but with additional knowledge (extra generics, known on root type).
 * <p>
 * Another special case is inner classes, which could reference outer class generics. To handle this, outer class
 * generics are also resolved. For inlying context building, if outer class present in current (root) hierarchy,
 * then known outer class generics will be used from the root hierarchy. Also, not that outer class generics could be
 * declared directly (e.g. field declaration: {@code Outer<String>.Inner field}) and, in this case, direct
 * declaration is used in priority.
 * <p>
 * It is impossible to to perform incorrect generics resolution through context api: it will either change context
 * automatically or fail with error (with detailed incompatibility explanation).
 *
 * @author Vyacheslav Rusakov
 * @since 17.11.2014
 */
// huge class size is OK, because it should be the only entry point for api
@SuppressWarnings({"PMD.ExcessiveClassLength", "PMD.PreserveStackTrace",
        "PMD.TooManyMethods", "PMD.GodClass"})
public abstract class AbstractGenericsContext {

    protected final GenericsInfo genericsInfo;
    protected final Class<?> currentType;
    protected final Map<String, Type> typeGenerics;

    public AbstractGenericsContext(final GenericsInfo genericsInfo, final Class<?> type) {
        this.genericsInfo = genericsInfo;
        this.currentType = type;
        // fail on wrong type
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
     * <p>
     * Note: always returns all declared class generics, even inside method or constructor context, where
     * class generics could be hidden by context generics with the same name.
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
     * <p>
     * Note: always returns all declared class generics, even inside method or constructor context, where
     * class generics could be hidden by context generics with the same name.
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
     * <p>
     * Note: always returns all declared class generics, even inside method or constructor context, where
     * class generics could be hidden by context generics with the same name.
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
     * <p>
     * Note: will work for any visible generic (from current context) including outer class, method
     * and constructor generics.
     *
     * @param genericName generic position (from 0)
     * @return generic type
     * @throws UnknownGenericException for wrong generic name
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
     * <p>
     * Note: will work for any visible generic (from current context) including outer class, method and
     * constructor generics.
     *
     * @param genericName generic name
     * @return resolved generic class
     * @throws UnknownGenericException for wrong generic name
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
     * <p>
     * Note: will work for any visible generic (from current context) including outer class and method generics
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
     * <p>
     * Note: always returns all declared class generics, even inside method or constructor context, where
     * class generics could be hidden by context generics with the same name.
     *
     * @return map of current type (runtime mapping of generic name to actual type)
     * @see #visibleGenericsMap() for all visible generics in current context
     */
    public Map<String, Type> genericsMap() {
        return new LinkedHashMap<String, Type>(typeGenerics);
    }

    /**
     * All reachable generics. Possible generics:
     * <ul>
     * <li>Context type generics ({@code Self<T, K>})</li>
     * <li>Outer type generics ({@code Outer<T>.Inner<K>})</li>
     * <li>Method generics ({@code <T> T get()})</li>
     * </ul>
     *
     * @return all generics visible from current context
     * @see #genericsMap() for type only generics
     * @see MethodGenericsContext#methodGenericsMap()
     * @see GenericsContext#ownerGenericsMap()
     */
    public Map<String, Type> visibleGenericsMap() {
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
     * <p>
     * Check if type containing generics, belonging to different context in current hierarchy and
     * automatically change context to properly resolve generics. Fails when it is impossible to correctly
     * resolve generics (preventing incorrect usage).
     *
     * @param type type to resolve class
     * @return resolved type class
     * @throws WrongGenericsContextException if type contains generics not visible from current class
     */
    public Class<?> resolveClass(final Type type) {
        return GenericsUtils.resolveClass(type, chooseContext(type).contextGenerics());
    }

    /**
     * Shortcut for {@link #resolveClass(Type)} for fields (same as {@code resolveClass(field.getGenericType()}).
     * Use for more informative error message on incorrect usage.
     * <p>
     * Automatically choose correct context or fail if field does not belong to current hierarchy.
     *
     * @param field field to resolve type
     * @return resolved type class
     * @throws IllegalArgumentException if field not belongs to any class in hierarchy
     */
    public Class<?> resolveFieldClass(final Field field) {
        return switchContext4field(field).resolveClass(field.getGenericType());
    }

    /**
     * Useful for introspection, to know exact generic value.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     List<T> doSmth();
     * }}</pre>
     * Resolving parameters in context of root class:
     * {@code type(B.class).resolveGenericsOf(B.class.getMethod("doSmth").getGenericReturnType()) == [Long.class]}
     * <p>
     * Check if type containing generics, belonging to different context in current hierarchy and
     * automatically change context to properly resolve generics. Fails when it is impossible to correctly
     * resolve generics (preventing incorrect usage).
     *
     * @param type type to resolve generics
     * @return resolved generic classes or empty list if type does not use generics
     * @throws WrongGenericsContextException if type contains generics not visible from current class
     * @see #resolveTypeGenerics(Type) if complete types are required
     */
    public List<Class<?>> resolveGenericsOf(final Type type) {
        return GenericsUtils.resolveGenericsOf(type, chooseContext(type).contextGenerics());
    }

    /**
     * Shortcut for {@link #resolveGenericsOf(Type)} for fields (same as
     * {@code resolveGenericsOf(field.getGenericType())}.
     * Use for more informative error message on incorrect usage.
     * <p>
     * Automatically choose correct context or fail if field does not belong to current hierarchy.
     *
     * @param field field to resolve generics for
     * @return resolved field generics or empty list if field does not contain generics
     * @throws IllegalArgumentException if field not belongs to any class in hierarchy
     * @see #resolveFieldClass(Field) for complete example of possible issues
     */
    public List<Class<?>> resolveFieldGenerics(final Field field) {
        return switchContext4field(field).resolveGenericsOf(field.getGenericType());
    }

    /**
     * Shortcut for {@link #resolveGenericsOf(Type)} useful for single generic types or
     * when just first generic required.
     * <p>
     * Check if type containing generics, belonging to different context in current hierarchy and
     * automatically change context to properly resolve generics. Fails when it is impossible to correctly
     * resolve generics (preventing incorrect usage).
     *
     * @param type type to resolve generic
     * @return first resolved generic or null if type does not use generics
     */
    public Class<?> resolveGenericOf(final Type type) {
        final List<Class<?>> res = resolveGenericsOf(type);
        return res.isEmpty() ? null : res.get(0);
    }

    /**
     * Shortcut for {@link #resolveGenericOf(Type)} for fields
     * (same as {@code resolveGenericOf(field.getGenericType())}).
     * Use for more informative error message on incorrect usage.
     * <p>
     * Automatically choose correct context or fail if field does not belong to current hierarchy.
     *
     * @param field field to resolve generic for
     * @return resolved field generic or null if field does not declare generics
     * @throws IllegalArgumentException if field not belongs to any class in hierarchy
     * @see #resolveFieldClass(Field) for complete example of possible issues
     */
    public Class<?> resolveFieldGeneric(final Field field) {
        return switchContext4field(field).resolveGenericOf(field.getGenericType());
    }

    /**
     * {@code List<T> field} could be resolved as {@code resolveFiledType(field) == List<String> as ParameterizedType}
     * (if T == String).
     * <p>
     * Automatically choose correct context or fail if field does not belong to current hierarchy.
     *
     * @param field field to resolve generics for
     * @return field type with resolved generic variables
     * @throws IllegalArgumentException if field not belongs to any class in hierarchy
     */
    public Type resolveFieldType(final Field field) {
        return switchContext4field(field).resolveType(field.getGenericType());
    }

    /**
     * Replace all named variables in type with actual generics. For example, {@code ParameterizedType List<T>}
     * would become {@code ParameterizedType List<String>} (assuming generic T is defined as String).
     * More complex cases could arrive like {@code T[]} or {@code ? extends T}, but everything will be correctly
     * replaced by actual (known) types, so you could be sure that returned type contains all known type information
     * and does not contain variables.
     * <p>
     * Useful when complete type information is required elsewhere (for example, to create typed DI binding).
     * <p>
     * Check if type containing generics, belonging to different context in current hierarchy and
     * automatically change context to properly resolve generics. Fails when it is impossible to correctly
     * resolve generics (preventing incorrect usage).
     *
     * @param type type to resolve named generics in
     * @return type without named generics (replaced by known actual types)
     * @throws WrongGenericsContextException if type contains generics not visible from current class
     */
    public Type resolveType(final Type type) {
        return GenericsUtils.resolveTypeVariables(type, chooseContext(type).contextGenerics());
    }

    /**
     * Returns generics of type with all named variables replaced in types with actual generics. For example,
     * {@code ParameterizedType Map<String, List<T>>} would become
     * {@code [Class String, ParameterizedType List<String>]} (assumed generic T is defined as String).
     * <p>
     * Useful when complete generic types are required.
     * <p>
     * Check if type containing generics, belonging to different context in current hierarchy and
     * automatically change context to properly resolve generics. Fails when it is impossible to correctly
     * resolve generics (preventing incorrect usage).
     *
     * @param type type to return resolved generics of
     * @return type generics without variables
     * @throws WrongGenericsContextException if type contains generics not visible from current class
     * @see #resolveGenericsOf(Type) if just classes required
     */
    public List<Type> resolveTypeGenerics(final Type type) {
        final Map<String, Type> generics = chooseContext(type).contextGenerics();
        return Arrays.asList(
                GenericsUtils.resolveTypeVariables(GenericsUtils.getGenerics(type, generics), generics));
    }

    /**
     * Useful for reporting or maybe logging. Resolves all generics and compose resulted type as string.
     * <pre>{@code class A extends B<Long>;
     * class B<T> {
     *     List<T> doSmth();
     * }}</pre>
     * Resolving parameters in type of root class:
     * {@code type(B.class).toStringType(B.class.getMethod("doSmth").getGenericReturnType()) == "List<Long>"}
     * <p>
     * Check if type containing generics, belonging to different context in current hierarchy and
     * automatically change context to properly resolve generics. Fails when it is impossible to correctly
     * resolve generics (preventing incorrect usage).
     *
     * @param type to to get string of
     * @return string representation for resolved type
     * @throws WrongGenericsContextException if type contains generics not visible from current class
     */
    public String toStringType(final Type type) {
        return TypeToStringUtils.toStringType(type, chooseContext(type).contextGenerics());
    }

    /**
     * Navigates current context to specific type in class hierarchy.
     *
     * @param type class to navigate to
     * @return new context instance specific to requested class or current context if type is the same
     * @throws IllegalArgumentException if requested class not present in root class hierarchy
     */
    public abstract GenericsContext type(Class<?> type);

    /**
     * Navigates current context to specific method (type context is switched to method declaring class).
     * It is required because method could contain it's own generics.
     * For example, {@code <T> void myMethod(T arg);}.
     * <p>
     * Use context to work with method parameters, return type or resolving types inside method.
     *
     * @param method method in current class hierarchy to navigate to (may be method from subclass, relative to
     *               currently selected, in this case context type will be automatically switched)
     * @return new context instance specific to requested method
     * @throws IllegalArgumentException if requested method's declaration class is not present in current class
     *                                  hierarchy
     */
    public abstract MethodGenericsContext method(Method method);

    /**
     * Navigates current context to specific constructor (type context is switched to constructor declaring class).
     * It is required because constructor could contain it's own generics.
     * For example, {@code <T> MyType(T arg)}.
     * <p>
     * Use context to work with constructor arguments.
     *
     * @param constructor constructor in current class hierarchy to navigate to (may be constructor from subclass,
     *                    relative to currently selected, in this case context type is automatically switched)
     * @return new context instance specific to requested constructor
     * @throws IllegalArgumentException if requested constructor's declaration class is not present in current class
     *                                  hierarchy
     */
    public abstract ConstructorGenericsContext constructor(Constructor constructor);

    /**
     * Create generics context for field type (with correctly resolved root generics)."Drill down".
     * <pre>{@code class A<T> {
     *    private B<T> field;
     * }
     * class C extends A<String> {}}</pre>
     * Build generics context for field type (to continue analyzing field class fields):
     * {@code type(A.class).fieldType(A.class.getField("field")) == generics context of B<String>}
     * <p>
     * Note that, in contrast to direct resolution {@code GenericsResolver.resolve(B.class)}, actual root generic
     * would be counted for hierarchy resolution.
     *
     * @param field field in current class hierarchy to resolve type from (may be field from superclass, relative to
     *              currently selected, in this case context type will be automatically switched)
     * @return generics context for field type (inlying context)
     * @throws IllegalArgumentException if field not belongs to any class in hierarchy
     * @see #inlyingType(Type)
     */
    public GenericsContext fieldType(final Field field) {
        return switchContext4field(field).inlyingType(field.getGenericType());
    }

    /**
     * Create generics context for type extending field type. Case: need to analyze field value type, but declared
     * field type is generic sub type. In this situation we can build generics context from required root type
     * (implementation type) with unknown generics and inject known subtype generics into hierarchy.
     * This is useful when analyzing object instance (introspecting actual object).
     * <p>
     * Other than that, method is almost the same as {@link #fieldType(Field)}.
     *
     * @param field  field in current class hierarchy to resolve type from (may be field from superclass, relative to
     *               currently selected, in this case context type will be automatically switched)
     * @param asType required actual root type (extending field type)
     * @return generics context for required target type with correct field type's generics (inlying context)
     * @throws IllegalArgumentException if field not belongs to any class in hierarchy
     * @see #inlyingTypeAs(Type, Class)
     */
    public GenericsContext fieldTypeAs(final Field field, final Class<?> asType) {
        return switchContext4field(field).inlyingTypeAs(field.getGenericType(), asType);
    }

    /**
     * Build generics context for type in context of current class. This may be required to analyze class of
     * field or method return type ("drill down"): new generics hierarchy must be built, but with correct
     * resolution of root generics. For example:
     * <pre>{@code class A<T> {
     *    private C<T> field;
     * }
     * class C extends A<String> {}}</pre>
     * To continue analysis of field type: {@code type(A.class).inlyingType(A.class.getField("field")
     * .getGenericType()) == generics context of C<String>}
     * <p>
     * Use shortcut methods to simplify navigation: {@link #fieldType(Field)},
     * {@link MethodGenericsContext#returnType()}, {@link MethodGenericsContext#parameterType(int)} and
     * {@link ConstructorGenericsContext#parameterType(int)}.
     * <p>
     * Check if type containing generics, belonging to different context in current hierarchy and
     * automatically change context to properly resolve generics. Fails when it is impossible to correctly
     * resolve generics (preventing incorrect usage).
     * <p>
     * If provided type did not contains generic then cached type resolution will be used (the same as
     * {@code GenericsResolver.resolve(Target.class)} and if generics present then type will be built on each call.
     * <p>
     * Returned context holds reference to original (root) context: {@link GenericsContext#rootContext()}.
     * <p>
     * Ignored types, used for context creation, are counted (will also be ignored for inlying context building).
     *
     * @param type type to resolve hierarchy from (it must be generified type, resolved in current class)
     * @return generics context of type (inlying context)
     * @throws WrongGenericsContextException if type contains generics not visible from current class
     */
    public abstract GenericsContext inlyingType(Type type);

    /**
     * Build generics context for type extending some generic type in context of current class. This is required
     * when only base/abstract type is declared in class, but we need to build context for actual value class
     * use. In this case, we can build context from target implementation class (where generics are unknown) but
     * include known generics for middle type (represented by provided).
     * Useful for object instance analysis case (introspecting actual object).
     * <p>
     * Example case: field declaration is {@code SomeInterface<T> field} and we need to build inlying context
     * for actual field value class {@code SomeImpl<K> implements SomeInterface<K>}. Type of root generic K will be
     * tracked from known generic T (on interface), but even if it's not possible to track root generics, hierarchy
     * starting from known type (interface in this case) will use correct generic value. This way, all known
     * generics information is used.
     * <p>
     * Other than different target type, method is the same as {@link #inlyingType(Type)}. By analogy, shortcuts
     * are provided for simpler navigation: {@link #fieldTypeAs(Field, Class)},
     * {@link MethodGenericsContext#returnTypeAs(Class)}, {@link MethodGenericsContext#parameterTypeAs(int, Class)}
     * and {@link ConstructorGenericsContext#parameterTypeAs(int, Class)}.
     *
     * @param type   type to resolve actual generics from (it must be generified type, resolved in current class)
     * @param asType required target type to build generics context for (must include declared type as base class)
     * @return generics context for required target type with correct type's generics (inlying context)
     * @throws WrongGenericsContextException if type contains generics not visible from current class
     * @see #inlyingType(Type)
     */
    public abstract GenericsContext inlyingTypeAs(Type type, Class<?> asType);

    /**
     * Look for generic variables presence inside type and, if found, try to switch to correct context.
     * Switching is based on declaration source information, contained inside generic.
     * <p>
     * Note that {@link WrongGenericsContextException} used instead of {@link IllegalArgumentException}, as in
     * other cases, because it is more specific error: type may come from anywhere and it may be not so easy to
     * track it's correct usage.
     *
     * @param type type, possibly containing generic variables
     * @return correct context for generics resolution inside type or the same context if type does not contains
     * generics
     * @throws WrongGenericsContextException when it is impossible to resolve type in correct context
     */
    public abstract GenericsContext chooseContext(Type type);

    /**
     * For example, {@code class Root extends Base<String>} (and we resolve generics from Root):
     * {@code context.toStringCurrentClass() == "Root"} and
     * {@code context.type(Base.class).toStringCurrentClass() == "Base<String>"}.
     *
     * @return string representation of current class in hierarchy with known generics
     */
    public String toStringCurrentClass() {
        return TypeToStringUtils.toStringWithGenerics(currentType, typeGenerics);
    }

    /**
     * For example, {@code Base<T>}.
     * {@code context.type(Base.class).toStringCurrentClassDeclaration() == "Base<T>"}.
     *
     * @return current class declaration (with declared generic names)
     */
    public String toStringCurrentClassDeclaration() {
        return TypeToStringUtils.toStringWithNamedGenerics(currentType);
    }

    /**
     * For example, {@code CLASS} scope could resolve only class level generics, whereas
     * {@code METHOD} could see method context generics. May be used for current context differentiation
     * (instead of instanceof).
     *
     * @return maximum declaration scope for current context
     * @see #getGenericsSource()
     */
    public abstract GenericDeclarationScope getGenericsScope();

    /**
     * Declaration source could be: class, method or constructor.
     *
     * @return declaration source for current context
     * @see #getGenericsScope()
     */
    public abstract GenericDeclaration getGenericsSource();

    /**
     * @return resolved generics mapping for current context
     */
    protected abstract Map<String, Type> contextGenerics();

    /**
     * Used to switch to appropriate context or fail if type is not found in hierarchy.
     * <p>
     * Note that this method may seem useless as correct context is always selected based on type generics,
     * but method is required to show more specific error (better indicating context).
     *
     * @param target    target type
     * @param msgPrefix error message prefix, identifying place
     * @return correct context for type (may be current context instance)
     * @throws IllegalArgumentException when context can't be switched
     * @see #chooseContext(Type)
     */
    protected abstract GenericsContext switchContext(Class target, String msgPrefix);

    private GenericsContext switchContext4field(final Field field) {
        return switchContext(field.getDeclaringClass(), "Field '" + field.getName() + "'");
    }

    private String checkGenericName(final String genericName) {
        if (!contextGenerics().containsKey(genericName)) {
            throw new UnknownGenericException(currentType, genericName, null);
        }
        return genericName;
    }
}

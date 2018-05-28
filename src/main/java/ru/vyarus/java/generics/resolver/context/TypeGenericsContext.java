package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericInfoUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generics context of specific type (class) or inlying context (when root type context known).
 * <p>
 * Inlying type generics context. Used for types, resolved not from root class, but from generic type declaration
 * in context of existing generics context. For example, hierarchy, build for generified field type (
 * {@code private Something<T> something;}).
 *
 * @author Vyacheslav Rusakov
 * @see GenericsContext
 * @see #inlyingType(java.lang.reflect.Type)
 * @since 26.06.2015
 */
public class TypeGenericsContext extends GenericsContext {

    /**
     * Current hierarchy position marker (for to string).
     */
    public static final String CURRENT_POSITION_MARKER = "    <-- current";

    protected Class<?> ownerType;
    protected Map<String, Type> ownerGenerics;
    protected Map<String, Type> allTypeGenerics;

    private final GenericsContext root;


    public TypeGenericsContext(final GenericsInfo genericsInfo, final Class<?> type) {
        this(genericsInfo, type, null);
    }

    public TypeGenericsContext(final GenericsInfo genericsInfo, final Class<?> type, final GenericsContext root) {
        super(genericsInfo, type);
        separateOwnerGenerics();
        this.root = root;
    }

    /**
     * <pre>{@code class Owner<T> {
     *     class Inner {
     *         T field;
     *     }
     * }}</pre>.
     * For Inner class owner is Owner. For Owner class owner is null.
     * <p>
     * Note: interface class is always static (can't be inner, even if declared as inner).
     *
     * @return owner class if current class is inner, null otherwise
     * @see #ownerGenericsMap()
     */
    public Class<?> ownerClass() {
        return ownerType;
    }

    /**
     * @return root context (of class containing current type) for inlying context or null
     * @see #inlyingType(Type)
     */
    public GenericsContext rootContext() {
        return root;
    }

    /**
     * During reflection analysis it's common to review some internal type like field or method return type.
     * This types may be declared with context class generics, so to completely resolve this type we must know
     * outer class generics. Contexts for such types resolved with outer context generic knowledge
     * ({@link #inlyingType(Type)}) are called inlying. You can always access root context for inlying using
     * {@link #rootContext()}.
     *
     * @return true if context is inlying (context resolved with known outer context), false if context is resolved
     * from root class
     */
    public boolean isInlying() {
        return root != null;
    }

    /**
     * Inner class may use outer generics like this:
     * <pre>{@code class Owner<T> {
     *     class Inner {
     *         T field;
     *     }
     * }}</pre>.
     * <p>
     * NOTE: this is not all generics of owner type, but visible generics. For example, some owner generics
     * may be hidden by the same generic name used in inner type:
     * <pre>{@code class Owner<T> {
     *      // hides outer generic
     *     class Inner<T> {}
     *
     *     class InnerMethod {
     *          // outer generic not visible inside method
     *          <T> T get() {}
     *     }
     * }}</pre>
     *
     * @return owner type generics if context type is inner class or empty map if not inner class or
     * outer type does not contains generics
     * @see #ownerClass()
     */
    public Map<String, Type> ownerGenericsMap() {
        return new LinkedHashMap<String, Type>(ownerGenerics);
    }

    @Override
    public String toString() {
        return genericsInfo.toStringHierarchy(new TypeContextWriter());
    }

    // --------------------------------------------------------------------- navigation impl

    @Override
    public TypeGenericsContext type(final Class<?> type) {
        return type == currentType ? this : new TypeGenericsContext(genericsInfo, type, root);
    }

    @Override
    public MethodGenericsContext method(final Method method) {
        final GenericsContext context = chooseContext(method.getDeclaringClass(),
                "Method '" + method.getName() + "'");
        return new MethodGenericsContext(context.genericsInfo, method, root);
    }

    @Override
    public TypeGenericsContext inlyingType(final Type type) {
        final Class target = resolveClass(type);
        final GenericsInfo generics;

        if (target.getTypeParameters().length > 0 || couldRequireKnownOuterGenerics(type)) {
            // resolve class hierarchy in context (non cachable context)
            // can't be primitive here
            generics = GenericInfoUtils.create(this, type, genericsInfo.getIgnoredTypes());
        } else {
            // class without generics - use cachable context
            generics = GenericsInfoFactory.create(
                    // always build hierarchy for non primitive type
                    TypeUtils.wrapPrimitive(target), genericsInfo.getIgnoredTypes());
        }

        return new TypeGenericsContext(generics, target, this);
    }

    @Override
    public TypeGenericsContext inlyingTypeAs(final Type type, final Class<?> asType) {
        final Class target = resolveClass(type);
        final GenericsInfo generics;
        if (target.getTypeParameters().length > 0
                || couldRequireKnownOuterGenerics(type) || couldRequireKnownOuterGenerics(asType)) {
            // resolve class hierarchy in context and from higher type (non cachable context)
            // can't be primitive
            generics = GenericInfoUtils.create(this, type, asType, genericsInfo.getIgnoredTypes());
        } else {
            // class without generics - use cachable context
            generics = GenericsInfoFactory.create(
                    // always build hierarchy for non primitive type
                    TypeUtils.wrapPrimitive(asType), genericsInfo.getIgnoredTypes());
        }
        return new TypeGenericsContext(generics, asType, this);
    }

    @Override
    protected GenericsContext chooseContext(final Class target, final String msgPrefix) {
        try {
            // switch context to avoid silly mistakes (will fail if declaring type is not in hierarchy)
            return type(target);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format(
                    msgPrefix + " declaration type %s is not present in hierarchy of %s",
                    target.getSimpleName(), genericsInfo.getRootClass().getSimpleName()), ex);
        }
    }

    // ---------------------------------------------------------------------  / navigation impl


    @Override
    protected Map<String, Type> contextGenerics() {
        return allTypeGenerics;
    }

    /**
     * If current type is inner class then it could reference outer class generics and they were included
     * into complete generics map. Now we need to separate them back.
     * <p>
     * In case when outer generic name clashes with inner class generic outer generic is overridden by class generic
     * (become unreachable).
     */
    private void separateOwnerGenerics() {
        ownerType = (Class) TypeUtils.getOuter(currentType);
        ownerGenerics = GenericsUtils.extractOwnerGenerics(currentType, typeGenerics);
        if (ownerGenerics.isEmpty()) {
            allTypeGenerics = typeGenerics;
        } else {
            allTypeGenerics = new LinkedHashMap<String, Type>(typeGenerics);
            // remove owner generics from main set (ok to modify map because it's a copy)
            for (String key : ownerGenerics.keySet()) {
                typeGenerics.remove(key);
            }
        }
    }

    /**
     * Inner class could use outer class generics, and if outer class is known (in current hierarchy),
     * we can assume to use it's generics (correct for most cases, but may be corner cases).
     *
     * @param type type to check
     * @return true if type is inner and outer class is present in current hierarchy
     */
    private boolean couldRequireKnownOuterGenerics(final Type type) {
        final Type outer = TypeUtils.getOuter(type);
        // inner class may use generics of the root class
        return outer != null && genericsInfo.getComposingTypes().contains(resolveClass(outer));
    }

    /**
     * Hierarchy writer with root context info (if available).
     */
    public abstract class RootContextAwareTypeWriter extends GenericsInfo.DefaultTypeWriter {
        @Override
        @SuppressWarnings("PMD.UseStringBufferForStringAppends")
        public String write(final Class<?> type,
                            final Map<String, Type> generics,
                            final Class<?> owner,
                            final Map<String, Type> ownerGenerics,
                            final String shift) {
            String res = super.write(type, generics, owner, ownerGenerics, shift);
            if (root != null && type == genericsInfo.getRootClass()) {
                res += String.format("  resolved in context of %s",
                        TypeToStringUtils.toStringWithGenerics(rootContext().genericsInfo.getRootClass(),
                                rootContext().genericsMap()));
            }
            return res;
        }
    }

    /**
     * Hierarchy writer with current type identification and root context info (if available).
     */
    class TypeContextWriter extends RootContextAwareTypeWriter {

        @Override
        public String write(final Class<?> type,
                            final Map<String, Type> generics,
                            final Class<?> owner,
                            final Map<String, Type> ownerGenerics,
                            final String shift) {
            final String res = super.write(type, generics, owner, ownerGenerics, shift);
            final String pointer = type == currentType ? CURRENT_POSITION_MARKER : "";
            return res + pointer;
        }
    }
}

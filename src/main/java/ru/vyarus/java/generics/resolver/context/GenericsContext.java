package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.error.WrongGenericsContextException;
import ru.vyarus.java.generics.resolver.util.GenericInfoUtils;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.*;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Generics context of specific type (class) or inlying context (when root type context known).
 * See {@link AbstractGenericsContext} for more information.
 * <p>
 * Not merged with {@link AbstractGenericsContext} in order to separate more specific implementation details
 * from common generic utility methods.
 *
 * @author Vyacheslav Rusakov
 * @see AbstractGenericsContext
 * @see #inlyingType(java.lang.reflect.Type)
 * @since 26.06.2015
 */
public class GenericsContext extends AbstractGenericsContext {

    /**
     * Current hierarchy position marker (for toString).
     */
    public static final String CURRENT_POSITION_MARKER = "    <-- current";
    private static final PrintableGenericsMap PRINTABLE_GENERICS = new PrintableGenericsMap();

    protected Class<?> ownerType;
    protected Map<String, Type> ownerGenerics;
    protected Map<String, Type> allTypeGenerics;

    private final GenericsContext root;


    public GenericsContext(final GenericsInfo genericsInfo, final Class<?> type) {
        this(genericsInfo, type, null);
    }

    public GenericsContext(final GenericsInfo genericsInfo, final Class<?> type, final GenericsContext root) {
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
     * NOTE: contains only owner type generics, not hidden by inner class generics. For example:
     * <pre>{@code class Owner<T, K> {
     *      // hides outer generic
     *     class Inner<T> {}
     * }}</pre>
     * Here {@code ownerGenericsMap() == ["K": Object]} because owner generic "T" is overridden by inner class
     * declaration.
     * <p>
     * In method or constructor contexts, context specific generics may also override owner generics
     * (e.g. {@code <T> T method();}), but still all reachable by class owner generics wll be returned.
     * This is done for consistency: no matter what context, method will return the same map. The only exception
     * is {@link #visibleGenericsMap()} which return only actually visible generics from current context
     * (class, method or constructor).
     *
     * @return reachable owner type generics if context type is inner class or empty map if not inner class or
     * outer type does not contains generics
     * @see #ownerClass()
     */
    public Map<String, Type> ownerGenericsMap() {
        return ownerGenerics.isEmpty()
                ? Collections.<String, Type>emptyMap() : new LinkedHashMap<String, Type>(ownerGenerics);
    }

    @Override
    public String toString() {
        return genericsInfo.toStringHierarchy(new TypeContextWriter());
    }

    @Override
    public GenericDeclarationScope getGenericsScope() {
        return GenericDeclarationScope.CLASS;
    }

    @Override
    public GenericDeclaration getGenericsSource() {
        return currentClass();
    }

    // --------------------------------------------------------------------- navigation impl

    @Override
    public GenericsContext type(final Class<?> type) {
        return type == currentType ? this : new GenericsContext(genericsInfo, type, root);
    }

    @Override
    public MethodGenericsContext method(final Method method) {
        // no need for switch, just for more concrete error message
        final GenericsContext context = switchContext(method.getDeclaringClass(),
                String.format("Method '%s'", TypeToStringUtils.toStringMethod(method, PRINTABLE_GENERICS)));
        return new MethodGenericsContext(context.genericsInfo, method, root);
    }

    @Override
    public ConstructorGenericsContext constructor(final Constructor constructor) {
        // no need for switch, just for more concrete error message
        final GenericsContext context = switchContext(constructor.getDeclaringClass(),
                String.format("Constructor '%s'",
                        TypeToStringUtils.toStringConstructor(constructor, PRINTABLE_GENERICS)));
        return new ConstructorGenericsContext(context.genericsInfo, constructor, root);
    }

    @Override
    public GenericsContext inlyingType(final Type type) {
        // check type compatibility
        final GenericsContext root = chooseContext(type);
        final Class target = root.resolveClass(type);
        final GenericsInfo generics;

        if (target.getTypeParameters().length > 0 || couldRequireKnownOuterGenerics(root, type)) {
            // resolve class hierarchy in context (non cachable context)
            // can't be primitive here
            generics = GenericInfoUtils.create(root, type, genericsInfo.getIgnoredTypes());
        } else {
            // class without generics - use cachable context
            generics = GenericsInfoFactory.create(
                    // always build hierarchy for non primitive type
                    TypeUtils.wrapPrimitive(target), genericsInfo.getIgnoredTypes());
        }

        return new GenericsContext(generics, target, root);
    }

    @Override
    public GenericsContext inlyingTypeAs(final Type type, final Class<?> asType) {
        // check type compatibility
        final GenericsContext root = chooseContext(type);
        final Class target = root.resolveClass(type);
        final GenericsInfo generics;
        if (target.getTypeParameters().length > 0
                || couldRequireKnownOuterGenerics(root, type) || couldRequireKnownOuterGenerics(root, asType)) {
            // resolve class hierarchy in context and from higher type (non cachable context)
            // can't be primitive
            generics = GenericInfoUtils.create(root, type, asType, genericsInfo.getIgnoredTypes());
        } else {
            // class without generics - use cachable context
            generics = GenericsInfoFactory.create(
                    // always build hierarchy for non primitive type
                    TypeUtils.wrapPrimitive(asType), genericsInfo.getIgnoredTypes());
        }
        return new GenericsContext(generics, asType, root);
    }

    @Override
    public GenericsContext chooseContext(final Type type) {
        if (!(type instanceof Class)) {
            // find variable, incompatible with current context
            final TypeVariable var = GenericsUtils
                    .findIncompatibleVariable(type, currentType, getGenericsScope(), getGenericsSource());
            if (var != null) {
                final GenericDeclarationScope scope = GenericDeclarationScope.from(var.getGenericDeclaration());
                // scope == null only for currently impossible cases, but new sources may appear
                if (scope != null) {
                    final Class<?> target = genericsInfo
                            .findContextByDeclarationType(GenericsUtils.getDeclarationClass(var));

                    // found correct context in hierarchy - switching
                    if (target != null) {
                        final GenericsContext context;
                        switch (scope) {
                            case METHOD:
                                context = method((Method) var.getGenericDeclaration());
                                break;
                            case CONSTRUCTOR:
                                context = constructor((Constructor) var.getGenericDeclaration());
                                break;
                            default:
                                context = type(target);
                                break;
                        }
                        return context;
                    }
                }
                // can't switch - notify incompatible context
                throw new WrongGenericsContextException(type, var, currentType, genericsInfo);
            }
        }
        return this;
    }

    @Override
    protected GenericsContext switchContext(final Class target, final String msgPrefix) {
        try {
            // switch context to avoid silly mistakes (will fail if declaring type is not in hierarchy)
            return type(target);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(String.format(
                    msgPrefix + " declaration type %s is not present in current hierarchy:%n%s",
                    TypeToStringUtils.toStringType(target), genericsInfo.toString()), ex);
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
     * @param root correct context for type resolution
     * @param type type to check
     * @return true if type is inner and outer class is present in current hierarchy
     */
    private boolean couldRequireKnownOuterGenerics(final GenericsContext root, final Type type) {
        final Type outer = TypeUtils.getOuter(type);
        // inner class may use generics of the root class
        return outer != null && genericsInfo.getComposingTypes().contains(root.resolveClass(outer));
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

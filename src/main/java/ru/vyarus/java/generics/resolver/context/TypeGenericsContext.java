package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

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
     * @return root context (of class containing current type) for inlying context or null
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
     * only from root class
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
     *
     * @return owner type generics if context type is inner class or empty map if not inner class or
     * outer type does not contains generics
     */
    public Map<String, Type> ownerTypeGenericsMap() {
        return new LinkedHashMap<String, Type>(ownerGenerics);
    }

    @Override
    public String toString() {
        return genericsInfo.toStringHierarchy(isInlying() ? new InlyingContextWriter() : new TypeContextWriter());
    }

    @Override
    public TypeGenericsContext type(final Class<?> type) {
        return type == currentType ? this : new TypeGenericsContext(genericsInfo, type, root);
    }

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
     * Hierarchy writer with current type identification.
     */
    class TypeContextWriter extends GenericsInfo.DefaultTypeWriter {
        @Override
        public String write(final Class<?> type,
                            final Map<String, Type> generics,
                            final Class<?> owner,
                            final Map<String, Type> ownerGenerics,
                            final String shift) {
            final String pointer = type == currentType ? CURRENT_POSITION_MARKER : "";
            return super.write(type, generics, owner, ownerGenerics, shift) + pointer;
        }
    }

    /**
     * Hierarchy writer with current type identification and outer context info.
     */
    class InlyingContextWriter extends GenericsInfo.DefaultTypeWriter {

        @Override
        @SuppressWarnings("PMD.UseStringBufferForStringAppends")
        public String write(final Class<?> type,
                            final Map<String, Type> generics,
                            final Class<?> owner,
                            final Map<String, Type> ownerGenerics,
                            final String shift) {
            String res = super.write(type, generics, owner, ownerGenerics, shift);
            if (type == genericsInfo.getRootClass()) {
                res += String.format("  resolved in context of %s",
                        TypeToStringUtils.toStringWithGenerics(rootContext().getGenericsInfo().getRootClass(),
                                rootContext().genericsMap()));
            }
            return res + (type == currentType ? CURRENT_POSITION_MARKER : "");
        }
    }
}

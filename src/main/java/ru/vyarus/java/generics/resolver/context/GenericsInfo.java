package ru.vyarus.java.generics.resolver.context;

import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeUtils;

import java.lang.reflect.Type;
import java.util.*;

import static ru.vyarus.java.generics.resolver.util.TypeToStringUtils.toStringWithGenerics;

/**
 * Holds types hierarchy resolved generics information.
 * Contains not just types actually containing generics, but all types (so type without generics will reference empty
 * map). This was done for simplicity: all types from hierarchy are known and reference generics context is completely
 * safe for all types from hierarchy.
 * <p>
 * Maps also may hold outer type's generics (if type is inner class), because inner classes could access outer
 * generics ({@see ru.vyarus.java.generics.resolver.context.TypeGenericsContext} for separation logic).
 *
 * @author Vyacheslav Rusakov
 * @since 16.10.2014
 */
public class GenericsInfo {

    /**
     * Single shift marker used to identity hierarchy level for toString.
     */
    public static final String SHIFT_MARKER = "  ";

    private static final TypeWriter DEFAULT_WRITER = new DefaultTypeWriter();
    private static final String EXTENDS_MARKER = "extends ";
    private static final String IMPLEMENTS_MARKER = "implements ";

    private final Class<?> root;
    // super interface type -> generic name -> generic type (either class or parametrized type or generic array)
    private final Map<Class<?>, LinkedHashMap<String, Type>> types;
    private final Class[] ignoredTypes;

    public GenericsInfo(final Class<?> root,
                        final Map<Class<?>, LinkedHashMap<String, Type>> types,
                        final Class... ignoredTypes) {
        this.root = root;
        this.types = types;
        this.ignoredTypes = ignoredTypes;
    }

    /**
     * @return root class (from where generics resolution started)
     */
    public Class<?> getRootClass() {
        return root;
    }

    /**
     * @param type class to get generics for
     * @return map of resolved generics for class (base class or interface implemented by root class or nay subclass)
     * @throws IllegalArgumentException is requested class is not present in root class hierarchy
     */
    public Map<String, Type> getTypeGenerics(final Class<?> type) {
        if (!types.containsKey(type)) {
            throw new IllegalArgumentException(String.format("Type %s is not assignable from %s",
                    type.getName(), root.getName()));
        }
        return new LinkedHashMap<String, Type>(types.get(type));
    }

    /**
     * @return list of all classes (and interfaces) of root class hierarchy
     */
    public Set<Class<?>> getComposingTypes() {
        return new HashSet<Class<?>>(types.keySet());
    }

    /**
     * @return types ignored from analysis (all specified types to ignore)
     */
    public Class[] getIgnoredTypes() {
        return Arrays.copyOf(ignoredTypes, ignoredTypes.length);
    }

    /**
     * @return all known types in hierarchy with known generics
     */
    public Map<Class<?>, LinkedHashMap<String, Type>> getTypesMap() {
        return new HashMap<Class<?>, LinkedHashMap<String, Type>>(types);
    }

    /**
     * @return current hierarchy with resolved generics
     * @see #toStringHierarchy(TypeWriter) for customized output
     */
    @Override
    public String toString() {
        return toStringHierarchy(DEFAULT_WRITER);
    }

    /**
     * Write current root class hierarchy with resolved generics.
     * Use custom writer to modify type rendering (on each line).
     *
     * @param typeWriter custom type writer
     * @return current hierarchy with resolved generics
     */
    public String toStringHierarchy(final TypeWriter typeWriter) {
        final StringBuilder res = new StringBuilder(types.size() * 50);
        toStringHierarchy(root, "", "", res, typeWriter);
        return res.toString();
    }

    private void toStringHierarchy(final Class<?> type,
                                   final String shift,
                                   final String prefix,
                                   final StringBuilder res,
                                   final TypeWriter typeWriter) {
        final LinkedHashMap<String, Type> generics = types.get(type);
        final Map<String, Type> ownerGenerics = GenericsUtils.getOwnerGenerics(type, generics);
        final Class<?> outer = TypeUtils.getOuter(type);
        res.append(String.format("%s%s%s%n",
                shift, prefix.isEmpty() ? (type.isInterface() ? "interface " : "class ") : prefix,
                typeWriter.write(type, GenericsUtils.getSelfGenerics(generics, ownerGenerics),
                        outer, ownerGenerics, shift)));
        final Class<?> superclass = type.getSuperclass();
        // not ignored (or not last)
        if (types.containsKey(superclass)) {
            toStringHierarchy(superclass, shift + SHIFT_MARKER, EXTENDS_MARKER, res, typeWriter);
        }
        for (Class<?> iface : type.getInterfaces()) {
            // not ignored
            if (types.containsKey(iface)) {
                toStringHierarchy(iface, shift + SHIFT_MARKER,
                        type.isInterface() ? EXTENDS_MARKER : IMPLEMENTS_MARKER, res, typeWriter);
            }
        }
    }

    /**
     * Customization interface to control types hierarchy to string behaviour. Internally used to mark
     * current position inside hierarchy for actual context implementations.
     * Used with {@link #toStringHierarchy(TypeWriter)}.
     */
    public interface TypeWriter {

        /**
         * Hierarchy is printed from the root class, each class on new line with shift (to represent hierarchy).
         * Only inner part is customizable: extends or implements marker will always be printed before produced
         * line and proper shift applied. Also, new line is always applied after.
         * <p>
         * For example,
         * <pre>{@code extends Base1<Model>
         *   extends Lvl2Base1<Model>
         * }</pre>
         * only {@code Base1} and {@code Lvl2Base1<Model>} will be rendered by writer.
         *
         * @param type          current type
         * @param generics      current type generics or empty map
         * @param owner         owner type if current is inner class or null if not
         * @param ownerGenerics owner generic if inner class
         * @param shift         current left shift (space) to be able to insert multiple lines
         * @return formatted class line (block)
         */
        String write(Class<?> type, Map<String, Type> generics,
                     Class<?> owner, Map<String, Type> ownerGenerics, String shift);
    }

    /**
     * Default hierarchy writer implementation.
     */
    public static class DefaultTypeWriter implements TypeWriter {
        @Override
        public String write(final Class<?> type,
                            final Map<String, Type> generics,
                            final Class<?> owner,
                            final Map<String, Type> ownerGenerics,
                            final String shift) {
            final String inner = owner != null
                    ? String.format(
                    "(inner to %s)", toStringWithGenerics(owner, ownerGenerics))
                    : "";
            return String.format("%s %s", toStringWithGenerics(type, generics), inner);
        }
    }
}

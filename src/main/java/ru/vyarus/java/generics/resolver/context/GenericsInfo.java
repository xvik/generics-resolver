package ru.vyarus.java.generics.resolver.context;

import java.lang.reflect.Type;
import java.util.*;

/**
 * Holds types hierarchy resolved generics information.
 * Contains not just types actually containing generics, but all types (so type without generics will reference empty
 * map). This was done for simplicity: all types from hierarchy are known and reference generics context is completely
 * safe for all types from hierarchy.
 *
 * @author Vyacheslav Rusakov
 * @since 16.10.2014
 */
public class GenericsInfo {
    private final Class<?> root;
    // super interface type -> generic name -> generic type (either class or parametrized type or generic array)
    private final Map<Class<?>, LinkedHashMap<String, Type>> types;

    public GenericsInfo(final Class<?> root, final Map<Class<?>, LinkedHashMap<String, Type>> types) {
        this.root = root;
        this.types = types;
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
}

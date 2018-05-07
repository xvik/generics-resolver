package ru.vyarus.java.generics.resolver.context;

/**
 * Inlying type generics context. Used for types, resolved not from root class, but from generic type declaration
 * in context of existing generics context. For example, hierarchy, build for generified field type (
 * {@code private Something<T> something;}).
 *
 * @author Vyacheslav Rusakov
 * @see #inlyingType(java.lang.reflect.Type)
 * @since 06.05.2018
 */
public class InlyingTypeGenericsContext extends TypeGenericsContext {

    private final GenericsContext root;

    public InlyingTypeGenericsContext(final GenericsInfo genericsInfo,
                                      final Class<?> type,
                                      final GenericsContext root) {
        super(genericsInfo, type);
        this.root = root;
    }

    /**
     * @return root context (of class containing current type)
     */
    public GenericsContext rootContext() {
        return root;
    }
}

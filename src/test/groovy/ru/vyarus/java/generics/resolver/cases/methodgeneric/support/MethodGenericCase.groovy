package ru.vyarus.java.generics.resolver.cases.methodgeneric.support

/**
 * @author Vyacheslav Rusakov 
 * @since 23.06.2015
 */
class MethodGenericCase extends SubMethodGenericCase<Cloneable> {

    public <T> T test(Class<T> arg, T arg2) {
        return null;
    }

    public <T extends Serializable> T testBounded(Class<T> arg, T arg2) {
        return null;
    }

    public <T extends Serializable, K extends T> K testDoubleBounded(Class<K> arg, K arg2) {
        return null;
    }
}

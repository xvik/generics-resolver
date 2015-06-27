package ru.vyarus.java.generics.resolver.cases.methodgeneric.support

/**
 * @author Vyacheslav Rusakov 
 * @since 23.06.2015
 */
class SubMethodGenericCase<O> {

    public <T extends O> T testSub(Class<T> arg, T arg2) {
        return null;
    }

    public <T extends O, K extends T> T testSub2(Class<T> arg, K arg2) {
        return null;
    }
}

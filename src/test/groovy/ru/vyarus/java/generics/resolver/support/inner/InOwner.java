package ru.vyarus.java.generics.resolver.support.inner;

/**
 * @author Vyacheslav Rusakov
 * @since 21.05.2018
 */
public class InOwner<T> {

    public class Inner {
        T field;
    }

    public class InnerExt extends Inner {}
}

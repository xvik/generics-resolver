package ru.vyarus.java.generics.resolver.inlying.support.track

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
class RootScopeContradiction<T extends Integer> extends Target<T> {

    // just declare type here, for simplicity
    Target<String> target
}

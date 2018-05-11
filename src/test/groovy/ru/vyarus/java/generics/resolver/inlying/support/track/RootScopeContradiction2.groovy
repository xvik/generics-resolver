package ru.vyarus.java.generics.resolver.inlying.support.track

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
class RootScopeContradiction2 <T extends Integer, K extends T> extends Target<K> {

    // just declare type here, for simplicity
    Target<String> target
}

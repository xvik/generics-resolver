package ru.vyarus.java.generics.resolver.inlying.support.track

/**
 * @author Vyacheslav Rusakov
 * @since 10.05.2018
 */
class Nested<K> extends Target<List<K>> {

    // just declare type here, for simplicity
    Target<List<String>> target
}

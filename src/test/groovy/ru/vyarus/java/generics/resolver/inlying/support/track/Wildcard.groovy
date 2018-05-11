package ru.vyarus.java.generics.resolver.inlying.support.track

/**
 * @author Vyacheslav Rusakov
 * @since 10.05.2018
 */
class Wildcard<K> extends Target<K> {

    // just declare type here, for simplicity
    Target<? extends String> target
}

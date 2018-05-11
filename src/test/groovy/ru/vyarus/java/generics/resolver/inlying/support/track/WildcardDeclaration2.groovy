package ru.vyarus.java.generics.resolver.inlying.support.track

/**
 * @author Vyacheslav Rusakov
 * @since 10.05.2018
 */
class WildcardDeclaration2<A, B extends A> extends Target<A> {

    // just declare type here, for simplicity
    Target<String> target
}

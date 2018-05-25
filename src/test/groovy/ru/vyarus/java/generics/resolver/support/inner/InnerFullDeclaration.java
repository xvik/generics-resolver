package ru.vyarus.java.generics.resolver.support.inner;

/**
 * @author Vyacheslav Rusakov
 * @since 21.05.2018
 */
public class InnerFullDeclaration extends InOwner<Integer> {

    // this owner generics must be used as more specific
    // (instead of parent generics)
    InOwner<String>.Inner inner;

    // outer generic used from root
    InOwner.Inner inner2;

}



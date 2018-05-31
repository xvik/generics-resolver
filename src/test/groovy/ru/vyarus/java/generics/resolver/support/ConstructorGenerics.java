package ru.vyarus.java.generics.resolver.support;

/**
 * constructor generics are not allowed in groovy, so using java source
 *
 * @author Vyacheslav Rusakov
 * @since 30.05.2018
 */
public class ConstructorGenerics<P> {

    public <T> ConstructorGenerics(T arg) {
    }

    // hide class generic
    public <P extends Comparable> ConstructorGenerics(P arg){

    }
}

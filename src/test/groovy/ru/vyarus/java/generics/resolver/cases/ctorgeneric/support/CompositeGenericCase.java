package ru.vyarus.java.generics.resolver.cases.ctorgeneric.support;

import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * @author Vyacheslav Rusakov
 * @since 10.10.2019
 */
public class CompositeGenericCase<T extends List> {

    public <X extends HashMap<Integer, Character>> CompositeGenericCase(Callable<X> callable) {
    }

    public <X extends HashMap<T, Character>> CompositeGenericCase(List<X> list) {
    }
}

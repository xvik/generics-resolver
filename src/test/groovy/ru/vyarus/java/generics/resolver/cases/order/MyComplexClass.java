package ru.vyarus.java.generics.resolver.cases.order;

import java.util.Collection;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 17.12.2018
 */
public class MyComplexClass<T extends List<D>, D extends Collection<P>, P> extends Base<D> {
}

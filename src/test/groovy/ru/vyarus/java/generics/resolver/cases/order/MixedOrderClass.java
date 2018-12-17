package ru.vyarus.java.generics.resolver.cases.order;

import java.util.Collection;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 17.12.2018
 */
public class MixedOrderClass<T extends List<D>, P, D extends Collection<P>> extends Base<D> {
}

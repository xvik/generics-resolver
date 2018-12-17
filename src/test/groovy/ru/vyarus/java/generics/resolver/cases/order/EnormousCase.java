package ru.vyarus.java.generics.resolver.cases.order;

import java.util.Collection;
import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 17.12.2018
 */
public class EnormousCase<T extends List<K>, K, P extends Collection<T>> extends Base<P> {
}

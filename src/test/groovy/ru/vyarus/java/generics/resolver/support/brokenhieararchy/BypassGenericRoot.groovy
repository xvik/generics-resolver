package ru.vyarus.java.generics.resolver.support.brokenhieararchy

import java.util.concurrent.Callable

/**
 *
 * @author Vyacheslav Rusakov 
 * @since 11.02.2015
 */
class BypassGenericRoot<T extends Callable, K> extends BrokenHierarchyBase<T, K>
        implements BrokenHierarchyInterface<T, K> {
}

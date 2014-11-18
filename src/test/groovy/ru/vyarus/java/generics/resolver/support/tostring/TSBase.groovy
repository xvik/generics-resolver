package ru.vyarus.java.generics.resolver.support.tostring

import ru.vyarus.java.generics.resolver.support.Model

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2014
 */
interface TSBase<T, K extends List<SType<T, Model[]>>> {

    T[] doSomth();
}

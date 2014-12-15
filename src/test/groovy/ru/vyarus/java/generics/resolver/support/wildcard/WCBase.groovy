package ru.vyarus.java.generics.resolver.support.wildcard

/**
 * @author Vyacheslav Rusakov 
 * @since 15.12.2014
 */
interface WCBase<T, K> extends WCBaseLvl2<? extends T>{
    T get(K k);
    K get2(T k);
    List<T> get3();
}
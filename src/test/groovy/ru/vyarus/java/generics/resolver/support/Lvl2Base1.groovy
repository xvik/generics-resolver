package ru.vyarus.java.generics.resolver.support

/**
 * deep hierarchy interface with parametrization
 *
 * @author Vyacheslav Rusakov 
 * @since 16.10.2014
 */
public interface Lvl2Base1<I> {

    Integer doSomth()

    I doSomth2()

    List<I> doSomth3()

    void doSomth4(I a, int b)

    I[] doSomth5()
}
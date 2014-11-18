package ru.vyarus.java.generics.resolver.support

/**
 * @author Vyacheslav Rusakov 
 * @since 18.10.2014
 */
class BeanBase<T> extends Lvl2BeanBase<T> implements Lvl2Base1<T> {

    @Override
    Integer doSomth() {
        return null
    }

    @Override
    T doSomth2() {
        return null
    }

    @Override
    List<T> doSomth3() {
        return null
    }

    @Override
    void doSomth4(T a, int b) {
    }

    @Override
    T[] doSomth5() {
        return null
    }
}

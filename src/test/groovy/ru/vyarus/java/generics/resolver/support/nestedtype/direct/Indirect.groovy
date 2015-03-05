package ru.vyarus.java.generics.resolver.support.nestedtype.direct

import ru.vyarus.java.generics.resolver.support.nestedtype.NestedGenericType

/**
 * @author Vyacheslav Rusakov 
 * @since 05.03.2015
 */
interface Indirect<T> extends Direct<NestedGenericType<T>> {

}
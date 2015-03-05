package ru.vyarus.java.generics.resolver.support.nestedtype.direct

import ru.vyarus.java.generics.resolver.support.nestedtype.GenericType
import ru.vyarus.java.generics.resolver.support.nestedtype.NestedGenericType

/**
 * Same interface in hierarchy used with different generics
 *
 * @author Vyacheslav Rusakov 
 * @since 05.03.2015
 */
class BadRoot implements Indirect<Root>, Direct<NestedGenericType<GenericType>> {
}

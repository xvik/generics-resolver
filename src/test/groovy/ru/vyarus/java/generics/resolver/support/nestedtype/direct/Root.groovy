package ru.vyarus.java.generics.resolver.support.nestedtype.direct

import ru.vyarus.java.generics.resolver.support.nestedtype.GenericType
import ru.vyarus.java.generics.resolver.support.nestedtype.NestedGenericType

/**
 * Checking same type generics equals with generified generic and direct generic
 * (direct types are always repackaged)
 *
 * @author Vyacheslav Rusakov 
 * @since 05.03.2015
 */
class Root implements Indirect<GenericType>, Direct<NestedGenericType<GenericType>> {
}

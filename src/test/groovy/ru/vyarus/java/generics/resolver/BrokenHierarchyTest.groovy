package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.brokenhieararchy.BrokenHierarchyBase
import ru.vyarus.java.generics.resolver.support.brokenhieararchy.BrokenHierarchyInterface
import ru.vyarus.java.generics.resolver.support.brokenhieararchy.BrokenHierarchyRoot
import ru.vyarus.java.generics.resolver.support.brokenhieararchy.BypassGenericRoot
import spock.lang.Specification

import java.util.concurrent.Callable


/**
 * @author Vyacheslav Rusakov 
 * @since 11.02.2015
 */
class BrokenHierarchyTest extends Specification {
    def "Check broken hierarchy resolution"() {

        when: "resolving class with no generics set"
        GenericsContext context = GenericsResolver.resolve(BrokenHierarchyRoot).type(BrokenHierarchyBase)
        then: "generics resolved just from generic bound"
        context.generic("T") == Callable
        context.generic("K") == Object

        when: "resolving interface with no generics set"
        context = context.type(BrokenHierarchyInterface)
        then: "generics resolved just from generic bound"
        context.generic("T") == Callable
        context.generic("K") == Object
    }

    def "Check bypass generic case"() {

        when: "root class bypass it's own generics"
        GenericsContext context = GenericsResolver.resolve(BypassGenericRoot).type(BrokenHierarchyBase)
        then: "generic resolved from root generic bound, but generic bound become lower"
        context.generic("T") == Callable
        context.generic("K") == Object

        when: "resolving interface with no generics set"
        context = context.type(BrokenHierarchyInterface)
        then: "generics resolved just from generic bound"
        context.generic("T") == Callable
        context.generic("K") == Object
    }
}
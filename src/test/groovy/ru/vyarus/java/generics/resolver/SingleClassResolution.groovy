package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.TypeGenericsContext
import ru.vyarus.java.generics.resolver.support.brokenhieararchy.BrokenHierarchyInterface
import spock.lang.Specification

import java.util.concurrent.Callable

/**
 * @author Vyacheslav Rusakov 
 * @since 12.02.2015
 */
class SingleClassResolution extends Specification {

    def "Check root class generics resolved"() {

        when: "resolving single class without hierarchy"
        TypeGenericsContext context = GenericsResolver.resolve(BrokenHierarchyInterface)
        then: "root generics resolved from bounds"
        context.generic("T") == Callable
        context.generic("K") == Object


    }
}
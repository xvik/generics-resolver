package ru.vyarus.java.generics.resolver.cases.ctorgeneric

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.cases.ctorgeneric.support.CompositeGenericCase
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import spock.lang.Specification

import java.util.concurrent.Callable

/**
 * @author Vyacheslav Rusakov
 * @since 10.10.2019
 */
class CompositeConstructorGenericTest extends Specification {

    def "Check composite constructor generic support"() {

        when: "resolving constructor context with composite generic"
        def context = GenericsResolver.resolve(CompositeGenericCase)
                .constructor(CompositeGenericCase.getConstructor(Callable))
        then: "no type info loose"
        context.constructorGenericsMap().get("X") == new ParameterizedTypeImpl(HashMap, Integer, Character)
        context.resolveParameterType(0) == new ParameterizedTypeImpl(Callable, new ParameterizedTypeImpl(HashMap, Integer, Character))
    }

    def "Check constructor generic depends on class generic"() {

        when: "resolving constructor which depends on class generic"
        def context = GenericsResolver.resolve(CompositeGenericCase)
                .constructor(CompositeGenericCase.getConstructor(List))
        then: "no type info loose"
        context.constructorGenericsMap().get("X") == new ParameterizedTypeImpl(HashMap, List, Character)
        context.resolveParameterType(0) == new ParameterizedTypeImpl(List, new ParameterizedTypeImpl(HashMap, List, Character))
    }
}

package ru.vyarus.java.generics.resolver.cases.methodgeneric

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import spock.lang.Specification

import java.util.concurrent.Callable

/**
 * @author Vyacheslav Rusakov
 * @since 10.10.2019
 */
class CompositeMethodGenericTest extends Specification {

    def "Check composite method generic support"() {

        when: "resolving method context with composite generic"
        def context = GenericsResolver.resolve(CompositeGenericCase)
                .method(CompositeGenericCase.getMethod("x", Callable))
        then: "no type info loose"
        context.methodGenericsMap().get("X") == new ParameterizedTypeImpl(HashMap, Integer, Character)
        context.resolveParameterType(0) == new ParameterizedTypeImpl(Callable, new ParameterizedTypeImpl(HashMap, Integer, Character))
    }


    static class CompositeGenericCase {

        public <X extends HashMap<Integer, Character>> void x(Callable<X> callable) {
        }
    }
}

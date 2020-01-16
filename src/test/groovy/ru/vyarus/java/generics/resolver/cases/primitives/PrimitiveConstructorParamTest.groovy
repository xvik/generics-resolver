package ru.vyarus.java.generics.resolver.cases.primitives

import ru.vyarus.java.generics.resolver.GenericsResolver
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 16.01.2020
 */
class PrimitiveConstructorParamTest extends Specification {

    def "Check primitive constructor parameter resolution"() {

        Integer.class.getConstructors()[0].newInstance(new Integer(10))

        def context = GenericsResolver.resolve(Integer.class)
                .constructor(Integer.class.getConstructors()[0])

        when: "resolving sub context"
        def type = context.parameterType(0)
        then: "target primitive wrapped"
        type.currentClass() == Integer
    }

    def "Check primitives resolution"() {
        def context = GenericsResolver.resolve(Constr.class)
                .constructor(Constr.class.getConstructors()[0])
        
        expect: "primitives preserved"
        context.resolveParametersTypes() == [int, List, short]
        context.resolveParameterType(0) == int
        context.resolveParameters() == [int, List, short]

        and: "to string shows primitives"
        context.toStringConstructor() == "PrimitiveConstructorParamTest.Constr(int, List, short)"

        and: "contexts use wrappers"
        context.parameterType(0).currentClass() == Integer
    }

    static class Constr {

        Constr(int one, List two, short three) {
        }
    }
}

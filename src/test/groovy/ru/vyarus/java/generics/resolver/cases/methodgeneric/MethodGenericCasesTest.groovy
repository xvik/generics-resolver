package ru.vyarus.java.generics.resolver.cases.methodgeneric

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.MethodGenericCase
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.SubMethodGenericCase
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov 
 * @since 23.06.2015
 */
class MethodGenericCasesTest extends Specification {

    def "Check simple method generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase)
                .resolveParameters(MethodGenericCase.getMethod("test", Class, Object))
        then: "resolved"
        params == [Class, Object]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase)
                .resolveReturnClass(MethodGenericCase.getMethod("test", Class, Object))
        then: "resolved"
        res == Object
    }

    def "Check bounded method generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase)
                .resolveParameters(MethodGenericCase.getMethod("testBounded", Class, Serializable))
        then: "resolved"
        params == [Class, Serializable]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase)
                .resolveReturnClass(MethodGenericCase.getMethod("testBounded", Class, Serializable))
        then: "resolved"
        res == Serializable
    }

    def "Check double bounded method generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase)
                .resolveParameters(MethodGenericCase.getMethod("testDoubleBounded", Class, Serializable))
        then: "resolved"
        params == [Class, Serializable]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase)
                .resolveReturnClass(MethodGenericCase.getMethod("testDoubleBounded", Class, Serializable))
        then: "resolved"
        res == Serializable
    }

    def "Check bounded method with class generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase).type(SubMethodGenericCase)
                .resolveParameters(SubMethodGenericCase.getMethod("testSub", Class, Object))
        then: "resolved"
        params == [Class, Cloneable]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase).type(SubMethodGenericCase)
                .resolveReturnClass(SubMethodGenericCase.getMethod("testSub", Class, Object))
        then: "resolved"
        res == Cloneable
    }
}
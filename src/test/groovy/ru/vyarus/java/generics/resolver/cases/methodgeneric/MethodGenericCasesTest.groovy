package ru.vyarus.java.generics.resolver.cases.methodgeneric

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.MethodGenericCase
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.SubMethodGenericCase
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import ru.vyarus.java.generics.resolver.context.TypeGenericsContext
import spock.lang.Specification

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType

/**
 * @author Vyacheslav Rusakov 
 * @since 23.06.2015
 */
class MethodGenericCasesTest extends Specification {

    def "Check simple method generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("test", Class, Object)).resolveParameters()
        then: "resolved"
        params == [Class, Object]

        when: "parameter with method generic"
        def type = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("test", Class, Object)).resolveParameterType(0)
        then: "resolved"
        type instanceof ParameterizedType
        ((ParameterizedType) type).rawType == Class
        ((ParameterizedType) type).actualTypeArguments == [Object]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("test", Class, Object)).resolveReturnClass()
        then: "resolved"
        res == Object

        when: "returned resul type"
        type = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("test", Class, Object)).resolveReturnType()
        then: "resolved"
        type == Object
    }

    def "Check bounded method generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("testBounded", Class, Serializable)).resolveParameters()
        then: "resolved"
        params == [Class, Serializable]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("testBounded", Class, Serializable)).resolveReturnClass()
        then: "resolved"
        res == Serializable
    }

    def "Check double bounded method generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("testDoubleBounded", Class, Serializable)).resolveParameters()
        then: "resolved"
        params == [Class, Serializable]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase)
                .method(MethodGenericCase.getMethod("testDoubleBounded", Class, Serializable)).resolveReturnClass()
        then: "resolved"
        res == Serializable
    }

    def "Check bounded method with class generic"() {

        when: "parameter with method generic"
        List<Class> params = GenericsResolver.resolve(MethodGenericCase).type(SubMethodGenericCase)
                .method(SubMethodGenericCase.getMethod("testSub", Class, Object)).resolveParameters()
        then: "resolved"
        params == [Class, Cloneable]

        when: "return type with method generic"
        Class res = GenericsResolver.resolve(MethodGenericCase).type(SubMethodGenericCase)
                .method(SubMethodGenericCase.getMethod("testSub", Class, Object)).resolveReturnClass()
        then: "resolved"
        res == Cloneable
    }

    def "Check direct method generic resolution fail avoid"() {

        setup:
        Method method = MethodGenericCase.getMethod("test", Class, Object)
        TypeGenericsContext context = GenericsResolver.resolve(MethodGenericCase)

        when: 'resolve generic from type with method generic'
        def res = context.resolveGenericOf(method.getGenericParameterTypes()[0])
        then: "context auto switched"
        res == Object

        when: 'resolving generic from method context'
        res = context.method(method).resolveGenericOf(method.getGenericParameterTypes()[0])
        then: 'resolved'
        res == Object
    }

    def "Check method generics map"() {

        when: "looking for method generics"
        MethodGenericsContext context = GenericsResolver.resolve(MethodGenericCase).type(SubMethodGenericCase)
                .method(SubMethodGenericCase.getMethod("testSub", Class, Object))

        then:
        context.methodGenericsMap() == ["T": Cloneable.class]
        context.methodGenericTypes() == [Cloneable.class]
        context.toStringMethod() == "Cloneable testSub(Class<Cloneable>, Cloneable)"

        when: "looking for complex method generics"
        context = GenericsResolver.resolve(MethodGenericCase).type(SubMethodGenericCase)
                .method(SubMethodGenericCase.getMethod("testSub2", Class, Object))

        then:
        context.methodGenericsMap() == ["T": Cloneable.class, "K": Cloneable.class]
        context.methodGenericTypes() == [Cloneable.class, Cloneable.class]
        context.toStringMethod() == "Cloneable testSub2(Class<Cloneable>, Cloneable)"
    }
}
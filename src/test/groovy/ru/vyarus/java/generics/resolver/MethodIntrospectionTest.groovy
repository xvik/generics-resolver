package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.*
import spock.lang.Specification
import spock.lang.Unroll

import java.lang.reflect.Method
import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2014
 */
class MethodIntrospectionTest extends Specification {

    @Unroll("Check #type methods introspection")
    def "Check methods introspection"() {

        when:
        GenericsContext context = GenericsResolver.resolve(root).type(bean)
        Method doSomth = bean.getMethod("doSomth")
        Method doSomth2 = bean.getMethod("doSomth2")
        Method doSomth3 = bean.getMethod("doSomth3")
        Method doSomth4 = bean.getMethod("doSomth4", Object, int)
        Method doSomth5 = bean.getMethod("doSomth5")
        Method doSomth6 = bean.getMethod("doSomth6")

        then: "check method return resolve"
        context.method(doSomth).resolveReturnClass() == Integer
        context.method(doSomth2).resolveReturnClass() == Model
        context.method(doSomth3).resolveReturnClass() == List
        context.method(doSomth4).resolveReturnClass() == void
        context.method(doSomth5).resolveReturnClass() == Model[]

        then: "check method params resolve"
        context.method(doSomth).resolveParameters() == []
        context.method(doSomth4).resolveParameters() == [Model, int]

        then: "check sub generic resolution"
        context.resolveGenericOf(doSomth3.genericReturnType) == Model
        context.resolveGenericsOf(doSomth3.genericReturnType) == [Model]
        context.resolveGenericsOf(doSomth6.getGenericReturnType()) == [Model, Model]

        then: "check type class resolution"
        context.resolveClass(doSomth4.genericParameterTypes[0]) == Model

        when: "check entire type resolution"
        def res = context.type(doSomth3.getDeclaringClass()).resolveType(doSomth3.getGenericReturnType())
        then: "resolved"
        doSomth3.getGenericReturnType() instanceof ParameterizedType
        ((ParameterizedType) doSomth3.getGenericReturnType()).getActualTypeArguments()[0] instanceof TypeVariable
        res instanceof ParameterizedType
        ((ParameterizedType) res).getActualTypeArguments()[0] == Model

        when: "resolve generic of no generic type"
        res = context.resolveGenericOf(doSomth.genericReturnType)
        then:
        res == null

        when: "resolve generic of no generic type 2"
        res == context.resolveGenericOf(doSomth2.genericReturnType)
        then:
        res == null

        where:
        type        | root     | bean
        "interface" | Root     | Lvl2Base1
        "bean"      | BeanRoot | BeanBase
    }

    def "Check array resolution"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Root).type(ComplexGenerics2)
        Method doSomth = ComplexGenerics2.getMethod("doSomth")
        Method doSomth2 = ComplexGenerics2.getMethod("doSomth2")

        then:
        context.method(doSomth).resolveReturnClass() == Model[]
        context.method(doSomth2).resolveReturnClass() == Model[][]
    }
}
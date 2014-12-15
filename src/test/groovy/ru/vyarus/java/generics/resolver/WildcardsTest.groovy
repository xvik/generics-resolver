package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.Model
import ru.vyarus.java.generics.resolver.support.wildcard.WCBase
import ru.vyarus.java.generics.resolver.support.wildcard.WCBaseLvl2
import ru.vyarus.java.generics.resolver.support.wildcard.WCRoot
import spock.lang.Specification

import java.lang.reflect.Method


/**
 * @author Vyacheslav Rusakov 
 * @since 15.12.2014
 */
class WildcardsTest extends Specification {

    def "Check wildcard generics resolution"() {

        when: "analyzing hierarchy with wildcards"
        GenericsContext context= GenericsResolver.resolve(WCRoot).type(WCBase)
        then: "correct generic values resolved"
        context.generic(0) == Model
        context.generic(1) == Object

        when: "analyzing methods with wildcards"
        Method get = WCBase.getMethod("get", Object)
        Method get2 = WCBase.getMethod("get2", Object)
        Method get3 = WCBase.getMethod("get3")
        then: "correct generics resolved"
        context.resolveReturnClass(get) == Model
        context.resolveReturnClass(get2) == Object
        context.resolveReturnClass(get3) == List

        context.resolveParameters(get) == [Object]
        context.resolveParameters(get2) == [Model]

        context.resolveGenericOf(get3.getGenericReturnType()) == Model


        when: "analyzing complex wildcards"
        context = context.type(WCBaseLvl2)
        then: "correct values returned"
        context.generic(0) == Model
    }
}
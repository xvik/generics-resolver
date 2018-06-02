package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.TypeGenericsContext
import ru.vyarus.java.generics.resolver.support.Model
import ru.vyarus.java.generics.resolver.support.wildcard.WCBase
import ru.vyarus.java.generics.resolver.support.wildcard.WCBaseLvl2
import ru.vyarus.java.generics.resolver.support.wildcard.WCRoot
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils
import spock.lang.Specification

import java.lang.reflect.Method
import java.lang.reflect.Type
import java.lang.reflect.WildcardType

/**
 * @author Vyacheslav Rusakov 
 * @since 15.12.2014
 */
class WildcardsTest extends Specification {

    def "Check wildcard generics resolution"() {

        when: "analyzing hierarchy with wildcards"
        TypeGenericsContext context = GenericsResolver.resolve(WCRoot).type(WCBase)
        then: "correct generic values resolved"
        context.generic(0) == Model
        context.generic(1) == Object

        when: "analyzing methods with wildcards"
        Method get = WCBase.getMethod("get", Object)
        Method get2 = WCBase.getMethod("get2", Object)
        Method get3 = WCBase.getMethod("get3")
        Method get4 = WCBase.getMethod("get4")
        then: "correct generics resolved"
        context.method(get).resolveReturnClass() == Model
        context.method(get2).resolveReturnClass() == Object
        context.method(get3).resolveReturnClass() == List

        context.method(get).resolveParameters() == [Object]
        context.method(get2).resolveParameters() == [Model]

        context.resolveGenericOf(get3.getGenericReturnType()) == Model
        context.method(get3).resolveReturnTypeGeneric() == Model
        context.resolveGenericOf(get4.getGenericReturnType()) == Model
        context.method(get4).resolveReturnTypeGeneric() == Model


        when: "analyzing complex wildcards"
        context = context.type(WCBaseLvl2)
        then: "correct values returned"
        context.generic(0) == Model
    }

    def "Check multiple bounds wildcard"() {

        when: "when having wildcard with multiple bounds"
        def res = GenericsResolutionUtils.resolveRawGenerics(Sample)

        then: "generic is resolved as wildcard with multiple bounds"
        res["T"] instanceof WildcardType
        ((WildcardType) res["T"]).upperBounds == [Number, Comparable] as Type[]

        and: "wildcard to string"
        res["T"].toString() == "? extends Number & Comparable"

    }

    def "Check transitive bounds resolution"() {

        when: "when transitive wildcards"
        def res = GenericsResolutionUtils.resolveRawGenerics(Sample2)

        then: "generic is resolved as wildcard with multiple bounds"
        res["K"] instanceof WildcardType
        ((WildcardType) res["K"]).upperBounds == [Number, Comparable] as Type[]

        and: "wildcard to string"
        res["K"].toString() == "? extends Number & Comparable"
    }


    def "Check transitive multiple bounds resolution"() {

        when: "when transitive wildcards"
        def res = GenericsResolutionUtils.resolveRawGenerics(Sample3)

        then: "generic is resolved as wildcard with multiple bounds"
        res["K"] instanceof WildcardType
        ((WildcardType) res["K"]).upperBounds == [Number, Comparable, Serializable] as Type[]

        and: "wildcard to string"
        res["K"].toString() == "? extends Number & Comparable & Serializable"
    }

    def "Check object ignorance"() {

        when: "when having wildcard with object as first bound"
        def res = GenericsResolutionUtils.resolveRawGenerics(Sample4)

        then: "generic is resolved as single class"
        !(res["T"] instanceof WildcardType)
        res["T"] == Comparable

        and: "to string"
        res["T"].toString() == "interface java.lang.Comparable"
    }

    static class Sample<T extends Number & Comparable> {}

    static class Sample2<T extends Number & Comparable, K extends T> {}

    // impossible in java!
    static class Sample3<T extends Number & Comparable, K extends T & Serializable> {}

    static class Sample4<T extends Object & Comparable> {}
}
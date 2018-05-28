package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable
import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Base2
import ru.vyarus.java.generics.resolver.support.Root
import spock.lang.Specification

import java.lang.reflect.Type
import java.util.concurrent.Callable


/**
 * @author Vyacheslav Rusakov 
 * @since 05.03.2015
 */
class WrapperTypesEqualsTest extends Specification {

    def "Check GenericArrayType equals"() {
        GenericArrayTypeImpl arr1 = new GenericArrayTypeImpl(Integer)
        GenericArrayTypeImpl arr1_1 = new GenericArrayTypeImpl(Integer)

        GenericArrayTypeImpl arr2 = new GenericArrayTypeImpl(Number)

        expect:
        arr1.equals(arr1)
        arr1.equals(arr1_1)
        arr1.hashCode() == arr1_1.hashCode()
        arr1.hashCode() != arr2.hashCode()

        !arr2.equals(arr1)
        !arr1.equals(null)
    }

    def "Check ParameterizedType equals"() {

        // Callable<Integer>
        ParameterizedTypeImpl pt1 = new ParameterizedTypeImpl(Callable, [Integer] as Type[])
        ParameterizedTypeImpl pt1_1 = new ParameterizedTypeImpl(Callable, [Integer] as Type[])
        // Callable<Number>
        ParameterizedTypeImpl pt2 = new ParameterizedTypeImpl(Callable, [Number] as Type[])
        // Comparator<Integer>
        ParameterizedTypeImpl pt3 = new ParameterizedTypeImpl(Comparator, [Integer] as Type[])

        ParameterizedTypeImpl pt4 = new ParameterizedTypeImpl(Comparator, [Integer] as Type[], Object.class)
        ParameterizedTypeImpl pt5 = new ParameterizedTypeImpl(Comparator, [Integer] as Type[], Callable.class)

        expect:
        pt1.equals(pt1)
        pt1.equals(pt1_1)
        pt1.hashCode() == pt1_1.hashCode()
        pt1.hashCode() != pt2.hashCode()
        !pt1.equals(pt2)
        !pt1.equals(pt3)
        !pt4.equals(pt5)
        !pt1.equals(null)
    }

    def "Check WildcardType equals"() {
        // ? extends Callable
        WildcardTypeImpl w1 = new WildcardTypeImpl([Callable] as Type[], [] as Type[])
        WildcardTypeImpl w1_1 = new WildcardTypeImpl([Callable] as Type[], [] as Type[])

        // ? extends Runnable
        WildcardTypeImpl w2 = new WildcardTypeImpl([Runnable] as Type[], [] as Type[])
        WildcardTypeImpl w3 = new WildcardTypeImpl([Root] as Type[], [Base1] as Type[])
        WildcardTypeImpl w4 = new WildcardTypeImpl([Root] as Type[], [Base2] as Type[])

        expect:
        w1.equals(w1)
        w1.equals(w1_1)
        w1.hashCode() == w1_1.hashCode()
        w1.hashCode() != w2.hashCode()
        !w1.equals(w2)
        !w3.equals(w4)
        !w1.equals(null)
    }

    def "Check explicit type equals"() {

        when: "same name"
        ExplicitTypeVariable v1 = new ExplicitTypeVariable("T")
        ExplicitTypeVariable v2 = new ExplicitTypeVariable("T")
        then: "ok"
        v1.equals(v2)
        v1.hashCode() == v2.hashCode()

        when: "different name"
        v2 = new ExplicitTypeVariable("K")
        then: "not"
        !v1.equals(v2)
        v1.hashCode() != v2.hashCode()
        
        when: "same type"
        v1 = new ExplicitTypeVariable(Some.getTypeParameters()[0])
        v2 = new ExplicitTypeVariable(Some.getTypeParameters()[0])
        then: "ok"
        v1.equals(v2)
        v1.hashCode() == v2.hashCode()

        when: "same name, different source"
        v2 = new ExplicitTypeVariable(SomeOther.getTypeParameters()[0])
        then: "not"
        !v1.equals(v2)
        v1.hashCode() != v2.hashCode()
    }

    static class Some<T> {}
    static class SomeOther<T> {}
}
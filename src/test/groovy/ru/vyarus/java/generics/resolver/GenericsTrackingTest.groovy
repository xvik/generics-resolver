package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import spock.lang.Specification

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.WildcardType


/**
 * @author Vyacheslav Rusakov
 * @since 31.05.2018
 */
class GenericsTrackingTest extends Specification {

    def "Check generics tracking"() {

        expect:
        GenericsTrackingUtils.track(type, Known, ["T": val] as LinkedHashMap) == res

        where:
        type     | val                                     | res
        Direct   | String                                  | ["U": String]
        Sub      | new ParameterizedTypeImpl(List, String) | ["U": String]
        Wild     | String                                  | ["U": Object, "P": String]
        Multiple | String                                  | ["U": WildcardTypeImpl.upper(Number, Comparable), "P": String]
        Arr      | String[]                                | ["U": String]
        Reversed | new ParameterizedTypeImpl(List, String) | ["A": String, "B": new ParameterizedTypeImpl(List, String)]
    }

    def "Check tracking shortcut"() {

        expect:
        GenericsUtils.trackGenerics(type, val) == res

        where:
        type     | val                                     | res
        Direct   | param(Known, String)                    | param(Direct, String)
        Sub      | param(Known, param(List, String))       | param(Sub, String)
        Wild     | param(Known, String)                    | param(Wild, Object, String)
        Multiple | param(Known, String)                    | param(Multiple, upper(Number, Comparable), String)
        Arr      | param(Known, String[])                  | param(Arr, String)
        Reversed | param(Known, param(List, String))       | param(Reversed, String, param(List, String))
        Reversed | upper(param(Known, param(List, String)))| param(Reversed, String, param(List, String))
        Integer  | param(Comparable, Number)               | Integer
        param(Direct, Integer) | param(Known, Number)      | param(Direct, Integer)
        List  | upper(List, Iterable)                      | List
        List  | upper(List, param(Iterable, String))       | param(List, String)
        array(List) | array(param(Iterable, String))       | array(param(List, String))
        Integer[]       | Number[]                         | Integer[]
        MultiRoot       | upper(param(Middle1, Number), param(MiddleI, Integer)) | param(MultiRoot, Integer)
    }

    def "Check impossible tracking detection"() {

        when: "types not compatible"
        GenericsUtils.trackGenerics(String, Integer)
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Can't track type String generics because it's not assignable to Integer"

        when: "types in bad order"
        GenericsUtils.trackGenerics(Number, Integer)
        then:
        ex = thrown(IllegalArgumentException)
        ex.message == "Can't track type Number generics because it's not assignable to Integer"
    }

    static class Known<T> {}

    static class Direct<U> extends Known<U> {}

    static class Sub<U> extends Known<List<U>> {}

    static class Wild<U, P extends U> extends Known<P> {}

    static class Multiple<U extends Number & Comparable, P> extends Known<P> {}

    static class Arr<U> extends Known<U[]> {}

    static class Reversed<A, B extends List<A>> extends Known<B> {}


    ParameterizedType param(Class root, Type... types) {
        return new ParameterizedTypeImpl(root, types)
    }

    GenericArrayType array(Type type) {
        return new GenericArrayTypeImpl(type)
    }

    WildcardType upper(Type... types) {
        return WildcardTypeImpl.upper(types)
    }

    WildcardType lower(Type type) {
        return WildcardTypeImpl.lower(type)
    }


    static class Middle1<T> {}
    static interface MiddleI<T> {}
    static class MultiRoot<T> extends Middle1<T> implements MiddleI<T> {}
}
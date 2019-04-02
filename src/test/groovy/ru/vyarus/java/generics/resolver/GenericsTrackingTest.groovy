package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import spock.lang.Specification

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*



/**
 * @author Vyacheslav Rusakov
 * @since 31.05.2018
 */
class GenericsTrackingTest extends Specification {

    def "Check generics tracking"() {

        expect:
        GenericsTrackingUtils.track(type, Known, ["T": val] as LinkedHashMap) == res

        where:
        type     | val                 | res
        Direct   | String              | ["U": String]
        Sub      | param(List, String) | ["U": String]
        Wild     | String              | ["U": Object, "P": String]
        Multiple | String              | ["U": upper(Number, Comparable), "P": String]
        Arr      | String[]            | ["U": String]
        Reversed | param(List, String) | ["A": String, "B": param(List, String)]
    }

    def "Chcek resolution with known composite generic in known"() {

        when: "all generics are directly used too"
        def res = GenericsTrackingUtils.track(Root2, Known2, ["T": String, "K": param(List, String)] as LinkedHashMap)
        then:
        res == ["A": String, "B": param(List, String), "C": Object]

    }

    def "Check tracking shortcut"() {

        expect:
        GenericsUtils.trackGenerics(type, val) == res

        where:
        type                   | val                                                    | res
        Direct                 | param(Known, String)                                   | param(Direct, String)
        Sub                    | param(Known, param(List, String))                      | param(Sub, String)
        Wild                   | param(Known, String)                                   | param(Wild, Object, String)
        Multiple               | param(Known, String)                                   | param(Multiple, upper(Number, Comparable), String)
        Arr                    | param(Known, String[])                                 | param(Arr, String)
        Reversed               | param(Known, param(List, String))                      | param(Reversed, String, param(List, String))
        Reversed               | upper(param(Known, param(List, String)))               | param(Reversed, String, param(List, String))
        Integer                | param(Comparable, Number)                              | Integer
        param(Direct, Integer) | param(Known, Number)                                   | param(Direct, Integer)
        List                   | upper(List, Iterable)                                  | List
        List                   | upper(List, param(Iterable, String))                   | param(List, String)
        array(List)            | array(param(Iterable, String))                         | array(param(List, String))
        Integer[]              | Number[]                                               | Integer[]
        MultiRoot              | upper(param(Middle1, Number), param(MiddleI, Integer)) | param(MultiRoot, Integer)
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

    static class Middle1<T> {}

    static interface MiddleI<T> {}

    static class MultiRoot<T> extends Middle1<T> implements MiddleI<T> {}

    static class Known2<T, K> {}

    static class Root2<A, B extends List<A>, C> extends Known2<A, B> {}
}
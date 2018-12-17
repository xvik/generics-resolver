package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils
import spock.lang.Specification


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

    static class Known<T> {}

    static class Direct<U> extends Known<U> {}

    static class Sub<U> extends Known<List<U>> {}

    static class Wild<U, P extends U> extends Known<P> {}

    static class Multiple<U extends Number & Comparable, P> extends Known<P> {}

    static class Arr<U> extends Known<U[]> {}

    static class Reversed<A, B extends List<A>> extends Known<B> {}
}
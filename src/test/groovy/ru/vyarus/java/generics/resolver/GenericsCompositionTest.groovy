package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.util.GenericsUtils
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 05.06.2018
 */
class GenericsCompositionTest extends Specification {

    def "Check generic composition cases"() {

        when: "correct generics provided"
        def res = GenericsUtils.createGenericsMap(Sample, [String, Integer])
        then:
        res == ["T": String, "K": Integer]

        when: "resolving inner class generics"
        res = GenericsUtils.createGenericsMap(Sample.Sub, [Double])
        then:
        res == ["P": Double, "T": Object, "K": Object]

        when: "wrong generics count"
        GenericsUtils.createGenericsMap(Sample, [String])
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Can't build generics map for Sample with [class java.lang.String] because of incorrect generics count"
    }

    static class Sample<T, K> {

        class Sub<P> {}
    }
}
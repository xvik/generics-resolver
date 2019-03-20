package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 20.03.2019
 */
class TypeBoundsAssignabilityTest extends Specification {


    def "Check assignable bounds multi-match test"() {

        expect: "no single type on left matches all right types, but still types are compatible"
        TypeUtils.isAssignableBounds(
                [Integer, ObjectInput] as Class[],
                [Number, Comparable, ObjectInput] as Class[])
    }

    def "Check incompatible bounds comparison"() {

        when: "bad bound provided"
        TypeUtils.isAssignableBounds([Class] as Class[], [] as Class[])
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Incomplete bounds information: [class java.lang.Class] []"
    }
}

package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral
import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 18.05.2018
 */
class AssignableTest extends Specification {

    def "Check types compatibility"() {

        expect:
        TypeUtils.isCompatible(Number, WildcardTypeImpl.lower(Number))  // ? super Number
        !TypeUtils.isCompatible(Integer, WildcardTypeImpl.lower(Number))
        TypeUtils.isCompatible(Number, WildcardTypeImpl.upper(Number, Comparable))
        TypeUtils.isCompatible(Integer, WildcardTypeImpl.upper(Number, Comparable))

    }

    def "Check types assignability"() {

        expect: "wildcards correct"
        TypeUtils.isAssignable(Number, WildcardTypeImpl.lower(Number))
        !TypeUtils.isAssignable(Integer, WildcardTypeImpl.lower(Number))
        !TypeUtils.isAssignable(WildcardTypeImpl.lower(Number), Integer)
        TypeUtils.isAssignable(WildcardTypeImpl.upper(Number, Comparable), Number)
        !TypeUtils.isAssignable(WildcardTypeImpl.upper(Number, Comparable), Integer)
        !TypeUtils.isAssignable(Number, WildcardTypeImpl.upper(Number, Comparable))
        TypeUtils.isAssignable(Integer, WildcardTypeImpl.upper(Number, Comparable))
        TypeUtils.isAssignable(WildcardTypeImpl.lower(Number), WildcardTypeImpl.lower(Integer))
        !TypeUtils.isAssignable(WildcardTypeImpl.lower(Integer), WildcardTypeImpl.lower(Number))

        and: "primitives correct"
        TypeUtils.isAssignable(int, Integer)
        TypeUtils.isAssignable(int, Number)
        !TypeUtils.isAssignable(Number, int)
        TypeUtils.isAssignable(Integer, int)
    }

    def "Check assignability cases"() {

        expect:
        TypeUtils.isAssignable(left, right) == res

        where:
        left | right | res
        new TypeLiteral<Map<String, String>>(){}.getType() | new TypeLiteral<Map<Object, String>>(){}.getType() | true
        new TypeLiteral<Map<String, String>>(){}.getType() | new TypeLiteral<Map<Integer, String>>(){}.getType() | false
    }
}
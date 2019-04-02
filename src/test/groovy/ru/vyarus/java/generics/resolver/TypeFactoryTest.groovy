package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl

import java.lang.reflect.Type

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 02.04.2019
 */
class TypeFactoryTest extends Specification {

    def "Check type factory methods"() {

        expect:
        literal(new L<List<String>>(){}) == new ParameterizedTypeImpl(List, String)
        param(List, String) == new ParameterizedTypeImpl(List, String)
        param(List, [String] as Type[], Comparable) == new ParameterizedTypeImpl(List, [String] as Type[], Comparable)
        array(String) == new GenericArrayTypeImpl(String)
    }

    def "Literal definision check"() {

        when: "incorrect definition"
        literal(new L<String>())
        then:
        def ex = thrown(IllegalArgumentException)
    }
}

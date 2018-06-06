package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable
import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import spock.lang.Specification

import java.lang.reflect.Type
import java.lang.reflect.TypeVariable


/**
 * @author Vyacheslav Rusakov
 * @since 07.06.2018
 */
class ContainersCorrectnessTest extends Specification {

    def "Check types cases"() {

        expect:
        type1.equals(type1)
        type2.equals(type2)
        type1.equals(type2) == equal
        type1.hashCode().equals(type2.hashCode()) == equal

        where:
        type1                                                   | type2                                                   | equal
        param(List)                                             | param(List)                                             | true
        param(List)                                             | param(Set)                                              | false
        param(List, [String])                                   | param(List, [String])                                   | true
        param(List, [String])                                   | param(List, [Integer])                                  | false
        param(List, [String], Object)                           | param(List, [String], Object)                           | true
        param(List, [String], Object)                           | param(List, [String], String)                           | false
        param(List, [String], Object)                           | param(List, [String])                                   | false
        param(List, [String])                                   | param(List, [String], Object)                           | false
        param(List)                                             | List                                                    | false
        array(String)                                           | array(String)                                           | true
        array(String)                                           | array(Object)                                           | false
        upper(String)                                           | upper(String)                                           | true
        upper(String)                                           | upper(Integer)                                          | false
        lower(String)                                           | lower(String)                                           | true
        lower(String)                                           | lower(Integer)                                          | false
        upper(String)                                           | lower(String)                                           | false
        var(List.getTypeParameters()[0])                        | var(List.getTypeParameters()[0])                        | true
        var(List.getTypeParameters()[0])                        | var(List.getTypeParameters()[0].name)                   | false
        var(List.getTypeParameters()[0].name)                   | var(List.getTypeParameters()[0])                        | false
        var(List.getTypeParameters()[0])                        | var(Set.getTypeParameters()[0])                         | false
        var(List.getTypeParameters()[0])                        | List                                                    | false
        var("Y")                                                | var("Y")                                                | true
        var("Y")                                                | List                                                    | false
        var(List.getTypeParameters()[0]).getDeclarationSource() | var(List.getTypeParameters()[0]).getDeclarationSource() | true
    }

    def "Check incorrect parameterized creation"() {

        when: "create with null type"
        param(null)
        then:
        thrown(IllegalArgumentException)
    }

    def "Check incorrect array creation"() {

        when: "create with null type"
        array(null)
        then:
        thrown(IllegalArgumentException)
    }

    private ParameterizedTypeImpl param(Class type, List<Type> gen = [], Type owner = null) {
        new ParameterizedTypeImpl(type, gen.toArray(new Type[0]), owner)
    }

    private GenericArrayTypeImpl array(Type type) {
        new GenericArrayTypeImpl(type)
    }

    private WildcardTypeImpl upper(Type... types) {
        WildcardTypeImpl.upper(types)
    }

    private WildcardTypeImpl lower(Type type) {
        WildcardTypeImpl.lower(type)
    }

    private var(String name) {
        new ExplicitTypeVariable(name)
    }

    private var(TypeVariable var) {
        new ExplicitTypeVariable(var)
    }
}
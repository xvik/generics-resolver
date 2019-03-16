package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import spock.lang.Specification

import java.lang.reflect.ParameterizedType

/**
 * @author Vyacheslav Rusakov
 * @since 15.12.2018
 */
class TypeLiteralTest extends Specification {

    def "Check type declaration"() {
        when: "declare same type"
        def type = new TypeLiteral<List<String>>() {}
        then:
        TypeToStringUtils.toStringTypeIgnoringVariables(type.getType()) == "List<String>"
        type.hashCode() > 0

        when: "declare the same type"
        def type2 = new TypeLiteral<List<String>>() {}
        then: "types equal"
        type.equals(type2)
        type.hashCode() == type2.hashCode()
        type.toString() == type2.toString()

        when: "declare differet type"
        def type3 = new TypeLiteral<List<Integer>>() {}
        then: "types not equal"
        !type.equals(type3)
        type.hashCode() != type3.hashCode()
        type.toString() != type3.toString()
    }

    def "Check manual literal declaration"() {

        when: "declare from class"
        def type = TypeLiteral.from(List)
        then:
        type.type == List

        when: "declare from type"
        type = TypeLiteral.from(new ParameterizedTypeImpl(List, String))
        then:
        type.type instanceof ParameterizedType
        type.type.rawType == List
        type.type.actualTypeArguments == [String]
    }

    def "Check incorrect declaration"() {

        when: "declare without type"
        new TypeLiteral()
        then: "error"
        thrown(IllegalArgumentException)
    }
}

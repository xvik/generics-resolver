package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import spock.lang.Specification

import java.lang.reflect.Type

/**
 * @author Vyacheslav Rusakov
 * @since 31.05.2018
 */
class VariablesSearchTest extends Specification {

    def "Check variables search"() {

        def E = List.getTypeParameters()[0]

        expect: "variables found for all cases"
        GenericsUtils.findVariables(Object) == []
        GenericsUtils.findVariables(new ParameterizedTypeImpl(List, E)) == [E]
        GenericsUtils.findVariables(new ParameterizedTypeImpl(List, [] as Type[], E)) == [E]
        GenericsUtils.findVariables(new ParameterizedTypeImpl(List, [E] as Type[], E)) == [E, E]
        GenericsUtils.findVariables(WildcardTypeImpl.upper(E)) == [E]
        GenericsUtils.findVariables(WildcardTypeImpl.lower(E)) == [E]
        GenericsUtils.findVariables(new GenericArrayTypeImpl(E)) == [E]
    }
}

package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import spock.lang.Specification

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*


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
        GenericsUtils.findVariables(param(List, E)) == [E]
        GenericsUtils.findVariables(param(List, [] as Type[], E)) == [E]
        GenericsUtils.findVariables(param(List, [E] as Type[], E)) == [E]
        GenericsUtils.findVariables(upper(E)) == [E]
        GenericsUtils.findVariables(lower(E)) == [E]
        GenericsUtils.findVariables(array(E)) == [E]
        GenericsUtils.findVariables(new ExplicitTypeVariable(E)) == [E]
    }
}

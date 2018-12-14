package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import spock.lang.Specification

import java.lang.reflect.Type
import java.lang.reflect.TypeVariable

/**
 * @author Vyacheslav Rusakov
 * @since 14.12.2018
 */
class MatchVariablesTest extends Specification {

    def "Check simple variables resolution"() {

        when: "matching direct var"
        def res = match(Lvl2Base1.getMethod("doSomth2").getGenericReturnType(), String.class)
        then: "ok"
        res.size() == 1
        res.values()[0] == String.class
    }

    private Map<TypeVariable, Type> match(Type src, Type compare) {
        GenericsUtils.matchVariables(GenericsUtils.preserveVariables(src), compare)
    }
}

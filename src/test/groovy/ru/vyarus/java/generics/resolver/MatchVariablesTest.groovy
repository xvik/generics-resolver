package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral
import ru.vyarus.java.generics.resolver.util.TypeVariableUtils
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

        when: "matching direct var"
        res = match(Lvl2Base1.getMethod("doSomth3").getGenericReturnType(), new TypeLiteral<List<String>>(){}.getType())
        then: "ok"
        res.size() == 1
        res.values()[0] == String.class
    }

    def "Check incompatible match"() {

        when: "matching var"
        match(Lvl2Base1.getMethod("doSomth3").getGenericReturnType(), String.class)
        then: "incompatible"
        def ex = thrown(IncompatibleTypesException)
        ex.message == "Type List<I> variables can't be matched from type String because they are not compatible"

        when: "matching var"
        match(Lvl2Base1.getMethod("doSomth3").getGenericReturnType(), new TypeLiteral<Map<String, Object>>(){}.getType())
        then: "incompatible"
        ex = thrown(IncompatibleTypesException)
        ex.message == "Type List<I> variables can't be matched from type Map<String, Object> because they are not compatible"
    }

    private Map<TypeVariable, Type> match(Type src, Type compare) {
        TypeVariableUtils.matchVariables(TypeVariableUtils.preserveVariables(src), compare)
    }
}

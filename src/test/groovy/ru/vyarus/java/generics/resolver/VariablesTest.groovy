package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import ru.vyarus.java.generics.resolver.util.TypeVariableUtils
import spock.lang.Specification

import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable

/**
 * @author Vyacheslav Rusakov
 * @since 15.12.2018
 */
class VariablesTest extends Specification {

    def "Check root variables resolution"() {

        when: "resolve type preserving root generics"
        def res = TypeVariableUtils.trackRootVariables(Base1)
        then: "root variable preserved"
        res[Lvl2Base1]['I'] instanceof ExplicitTypeVariable
    }

    def "Check variables preserve"() {

        when: "type without variables"
        def res = TypeVariableUtils.preserveVariables(String)
        then: "type preserved"
        res == String

        when: "type contain variables"
        res = TypeVariableUtils.preserveVariables(Lvl2Base1.getMethod("doSomth3").getGenericReturnType())
        then: "variable replaced"
        ((ParameterizedType) res).actualTypeArguments[0] instanceof ExplicitTypeVariable
        res.toString() == "List<I>"
    }

    def "Check unknown explicit generic with utility"() {

        when: "resolve explicit variable normally"
        GenericsUtils.resolveTypeVariables(new ExplicitTypeVariable((TypeVariable) Lvl2Base1.getMethod("doSomth2").getGenericReturnType()), [:])
        then: "explicit variable preserved"

        when: "resolving with missed generic"
        TypeVariableUtils.resolveAllTypeVariables(new ExplicitTypeVariable((TypeVariable) Lvl2Base1.getMethod("doSomth2").getGenericReturnType()), [:])
        then:
        def ex = thrown(UnknownGenericException)
        ex.message == "Generic 'I' (defined on Lvl2Base1<I>) is not declared "
        ex.genericName == "I"
        ex.genericSource != null
    }
}

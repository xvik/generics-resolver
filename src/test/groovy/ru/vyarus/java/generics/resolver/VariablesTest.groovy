package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable
import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral
import ru.vyarus.java.generics.resolver.util.TypeVariableUtils
import spock.lang.Specification

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type
import java.lang.reflect.TypeVariable
import java.lang.reflect.WildcardType

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

    def "Check direct variables resolution as upper bounds"() {

        when: "resolving type with variable"
        def res = TypeVariableUtils.resolveAllTypeVariables(Lvl2Base1.getMethod("doSomth2").getGenericReturnType())
        then:
        res == Object

        when: "resolving type with variable"
        res = TypeVariableUtils.resolveAllTypeVariables(Lvl2Base1.getMethod("doSomth3").getGenericReturnType())
        then:
        res.toString() == "List"

        when: "resolving type with variable"
        res = TypeVariableUtils.resolveAllTypeVariables(BoundedRoot.getType())
        then:
        res.toString() == "List<String>"

        when: "type without variables"
        def type = String
        res = TypeVariableUtils.resolveAllTypeVariables(type)
        then:
        res == type

        when: "array with variable resolution"
        res = TypeVariableUtils.resolveAllTypeVariables(BoundedRoot.getArrayType())
        then: "flattened to class"
        res == String[]

        when: "wildcard with variable resolution"
        res = TypeVariableUtils.resolveAllTypeVariables(BoundedRoot.getWildcardType())
        then:
        res == String
    }

    def "Check preserved variables resolution"() {

        when: "resolving type with variable"
        def res = TypeVariableUtils.resolveAllTypeVariables(TypeVariableUtils.preserveVariables(BoundedRoot.getType()))
        then:
        res.toString() == "List<String>"

        when: "array with variable resolution"
        res = TypeVariableUtils.resolveAllTypeVariables(TypeVariableUtils.preserveVariables(BoundedRoot.getArrayType()))
        then:
        res == String[]

        when: "wildcard with variable resolution"
        res = TypeVariableUtils.resolveAllTypeVariables(TypeVariableUtils.preserveVariables(BoundedRoot.getWildcardType()))
        then:
        res == String
    }

    def "Check array of preserved vars resolution"() {

        when: "resolving array of types"
        def res = TypeVariableUtils.resolveAllTypeVariables([
                TypeVariableUtils.preserveVariables(BoundedRoot.getType()),
                TypeVariableUtils.preserveVariables(BoundedRoot.getArrayType()),
                TypeVariableUtils.preserveVariables(BoundedRoot.getWildcardType())
        ] as Type[], ["T": String, "K": String])
        then:
        res[0].toString() == "List<String>"
        res[1] == String[]
        res[2] == String
    }

    def "Check flatten cases"() {

        when: "empty parameterized type without"
        def res = GenericsUtils.resolveTypeVariables(new ParameterizedTypeImpl(String, [] as Type[]), [:])
        then: "flattenned"
        res == String

        when: "empty parameterized type with outer"
        res = GenericsUtils.resolveTypeVariables(new ParameterizedTypeImpl(String, [] as Type[], VariablesTest), [:])
        then: "not flattenned"
        res != String
        res instanceof ParameterizedType

        when: "empty wildcard type"
        res = GenericsUtils.resolveTypeVariables(WildcardTypeImpl.upper(String), [:])
        then: "flattenned"
        res == String

        when: "correct wildcard type"
        res = GenericsUtils.resolveTypeVariables(WildcardTypeImpl.upper(String, Cloneable), [:])
        then: "not flattenned"
        res != String
        res instanceof WildcardType

        when: "simple generic array"
        res = GenericsUtils.resolveTypeVariables(new GenericArrayTypeImpl(String), [:])
        then: "flattenned"
        res == String[]

        when: "flattened generic component array"
        res = GenericsUtils.resolveTypeVariables(new GenericArrayTypeImpl(WildcardTypeImpl.upper(String)), [:])
        then: "flattenned"
        res == String[]

        when: "complex generic array"
        res = GenericsUtils.resolveTypeVariables(new GenericArrayTypeImpl(new ParameterizedTypeImpl(List, String)), [:])
        then: "not flattenned"
        res != List[]
        res instanceof GenericArrayType
    }

    static class BoundedRoot<T extends String, K extends T> {

        static Type getType() {
            return new TypeLiteral<List<T>>() {}.getType()
        }

        static Type getArrayType() {
            return new TypeLiteral<T[]>() {}.getType()
        }

        static Type getWildcardType() {
            return new TypeLiteral<K>() {}.getType()
        }
    }
}

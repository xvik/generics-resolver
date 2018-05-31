package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericDeclarationScope
import ru.vyarus.java.generics.resolver.support.ConstructorGenerics
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 31.05.2018
 */
class ConstructorIntrospectionTest extends Specification {

    def "Check constructor context methods"() {

        def ctor = ConstructorGenerics.getConstructor(Object)

        when:
        def context = GenericsResolver.resolve(ConstructorGenerics).constructor(ctor)
        then:
        context.currentConstructor() == ctor
        context.constructorGenericTypes() == [Object]
        context.constructorGenericsMap() == ["T": Object]
        context.resolveParameters() == [Object]
        context.resolveParametersTypes() == [Object]
        context.resolveParameterType(0) == Object
        context.parameterType(0).currentClass() == Object
        context.parameterTypeAs(0, Serializable).currentClass() == Serializable
        context.toStringConstructor() == "ConstructorGenerics(Object)"
        context.getGenericsScope() == GenericDeclarationScope.CONSTRUCTOR
        context.getGenericsSource() == ctor

        when:
        ctor = ConstructorGenerics.getConstructor(Comparable)
        context = context.constructor(ctor)
        then:
        context.currentConstructor() == ctor
        context.constructorGenericTypes() == [Comparable]
        context.constructorGenericsMap() == ["P": Comparable]
        context.resolveParameters() == [Comparable]
        context.resolveParametersTypes() == [Comparable]
        context.resolveParameterType(0) == Comparable
        context.parameterType(0).currentClass() == Comparable
        context.parameterTypeAs(0, Integer).currentClass() == Integer
        context.toStringConstructor() == "ConstructorGenerics(Comparable)"
        context.getGenericsScope() == GenericDeclarationScope.CONSTRUCTOR
        context.getGenericsSource() == ctor
    }
}

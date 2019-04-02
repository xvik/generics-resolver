package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Model
import ru.vyarus.java.generics.resolver.support.Root
import spock.lang.Specification

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*


/**
 * @author Vyacheslav Rusakov
 * @since 05.06.2018
 */
class ContextTypesResolutionTest extends Specification {

    def "Check direct types resolution"() {

        GenericsContext context = GenericsResolver.resolve(Root)
        println(context)

        expect: "resolving type"
        context.resolveType(Lvl2Base1.getMethod("doSomth5").getGenericReturnType()) == Model[]
        context.resolveType(Lvl2Base1.getMethod("doSomth6").getGenericReturnType()) == param(Map, Model, Model)
    }

    def "Check generics resolution from type"() {

        GenericsContext context = GenericsResolver.resolve(Root)
        println(context)
        
        expect: "generics"
        context.resolveTypeGenerics(Lvl2Base1.getMethod("doSomth5").getGenericReturnType()).isEmpty()
        context.resolveTypeGenerics(Lvl2Base1.getMethod("doSomth6").getGenericReturnType()) == [Model, Model]

        and: "when called on raw class"
        context.resolveTypeGenerics(Lvl2Base1) == [Object]
    }
}
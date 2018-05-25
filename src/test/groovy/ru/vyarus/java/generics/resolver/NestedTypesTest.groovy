package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.error.GenericsResolutionException
import ru.vyarus.java.generics.resolver.support.clash.ClashRoot
import ru.vyarus.java.generics.resolver.support.nestedtype.NestedGenericType
import ru.vyarus.java.generics.resolver.support.nestedtype.RootClass
import ru.vyarus.java.generics.resolver.support.nestedtype.SubClass2
import ru.vyarus.java.generics.resolver.support.nestedtype.direct.BadRoot
import ru.vyarus.java.generics.resolver.support.nestedtype.direct.Direct
import ru.vyarus.java.generics.resolver.support.nestedtype.direct.Indirect
import ru.vyarus.java.generics.resolver.support.nestedtype.direct.Root
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils
import spock.lang.Specification

import java.lang.reflect.Type
import java.util.concurrent.Callable

/**
 * Generic type resolution should succeed, if a class implement the same interface
 * through multiple ways, if the resolved generic types are the same.
 *
 * SubClass2
 *   -&gt; SubClass1&lt;T&gt;         (T = GenericType)
 *     -&gt; RootClass&lt;T&gt;       (T = NestedGenericType&lt;GenericType&gt;)
 *       -&gt; RootInterface&lt;T&gt; (T = NestedGenericType&lt;GenericType&gt;)
 * SubClass2
 *   -&gt; SubClass1&lt;T&gt;         (T = GenericType)
 *     -&gt; SubInterface&lt;T&gt;    (T = NestedGenericType&lt;GenericType&gt;)
 *       -&gt; RootInterface&lt;T&gt; (T = NestedGenericType&lt;GenericType&gt;)
 *
 * @author Adam Biczok
 * @since 04.03.2015
 */
class NestedTypesTest extends Specification {

    def "Check generic resolution on duplicated interfaces if generic types are the same"() {

        when: "resolving generic type"
        GenericsContext context = GenericsResolver.resolve(SubClass2).type(RootClass)
        then: "correct generic values resolved"
        context.generic("T") == NestedGenericType

        when: "resolving type with indirectly and directly generified interfaces"
        context = GenericsResolver.resolve(ru.vyarus.java.generics.resolver.support.nestedtype.direct.Root).type(Direct)
        then: "correct resolution"
        context.generic("T") == NestedGenericType
    }

    def "Check duplicate interface with different generics"() {

        when: "resolving type hierarchy with duplicate interface and different generics"
        GenericsResolver.resolve(BadRoot)
        then: "error"
        def ex = thrown(GenericsResolutionException)
        ex.message == "Failed to analyze hierarchy for BadRoot"
        ex.getCause().message == "Interface Direct<T> appears multiple times in root class hierarchy with incompatible parametrization for generic T: NestedGenericType<GenericType> and NestedGenericType<Root>"

        when: "resolving type hierarchy with duplicate interface and different generics"

        GenericsResolutionUtils.resolve(BadRoot,
                [] as LinkedHashMap<String, Type>,
                [(Indirect): ["T": Root]] as Map<Class<?>, LinkedHashMap<String, Type>>,
                [] as List<Class>)
        then: "error"
        ex = thrown(GenericsResolutionException)
        ex.message == "Failed to analyze hierarchy for BadRoot (with known generics: Indirect<Root>)"
        ex.getCause().message == "Interface Direct<T> appears multiple times in root class hierarchy with incompatible parametrization for generic T: NestedGenericType<GenericType> and NestedGenericType<Root>"
    }

    def "Check different interface types merged"() {

        when: "analyzing hierarchy with duplicate interfaces"
        def res = GenericsResolver.resolve(ClashRoot)
        then: "resolved with upper generic"
        res.type(Callable).generic(0) == Integer

    }

    def "Check hierarchy is not tracked more then one level"() {

        // NOTE it is possible to track back more then one level, but it's too weird and hard to search problems

        when: "resolve 3 contexts"
        GenericsContext context = GenericsResolver.resolve(Root.class)
                                .fieldType(Root.getDeclaredField("sub"))
                                .fieldType(Sub.getDeclaredField("inner"))
        then: "generic was not tracked"
        context.resolveFieldClass(Owner.Inner.getDeclaredField("foo")) == Object
    }

    // Root context contains owner type
    // -> sub field context
    //      -> inner field context should you super.super context as owner
    static class Root extends Owner<String> {
        Sub sub
    }

    static class Owner<T> {
        class Inner {
            T foo
        }
    }

    static class Sub  {
        Owner.Inner inner
    }
}

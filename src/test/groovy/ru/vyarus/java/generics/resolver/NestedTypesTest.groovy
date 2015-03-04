package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.nestedtype.RootClass
import ru.vyarus.java.generics.resolver.support.nestedtype.SubClass2
import ru.vyarus.java.generics.resolver.support.nestedtype.NestedGenericType
import spock.lang.Specification

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
    }
}

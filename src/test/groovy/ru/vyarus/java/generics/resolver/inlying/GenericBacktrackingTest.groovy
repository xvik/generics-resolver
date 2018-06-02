package ru.vyarus.java.generics.resolver.inlying

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.error.GenericsTrackingException
import ru.vyarus.java.generics.resolver.inlying.support.track.*
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 09.05.2018
 */
class GenericBacktrackingTest extends Specification {

    def "Check generics tracking cases"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(Source)

        when: "multiple generics"
        def res = context.fieldTypeAs(Source.getDeclaredField("target"), MultipleGenerics)
        then: "one generic tracked"
        res.generic("A") == String
        res.generic("B") == Object

        when: "no track"
        res = context.fieldTypeAs(Source.getDeclaredField("target"), NoTrack)
        then: "nothing to track"
        res.generic("P") == Object
        and: "actual hierarchy generic was Object, but known generic was used"
        res.type(Target).generic("T") == String

        when: "known generic contradict actual generic value in class hierarchy"
        context.fieldTypeAs(Source.getDeclaredField("target"), ContradictionTrack)
        then: "contradiction detected"
        def ex = thrown(GenericsTrackingException)
        ex.message == "Failed to track generics of ContradictionTrack<P> from sub type Target<String>"
        ex.getCause().message == "Known generic T of Target<T> is not compatible with ContradictionTrack hierarchy: String when required Integer"

        when: "multiple tiers before known type"
        res = context.fieldTypeAs(Source.getDeclaredField("target"), MultiTier)
        then: "one generic tracked"
        res.generic("A") == String
        res.generic("B") == Object

    }

    def "Complex checks"() {

        when: "nested declaration"
        GenericsContext context = GenericsResolver.resolve(Nested)
        def res = context.fieldTypeAs(Nested.getDeclaredField("target"), Nested)
        then: "one generic tracked"
        res.genericAsString("K") == 'String'

        when: "nested declaration with non equal right"
        context = GenericsResolver.resolve(NestedRight)
        res = context.fieldTypeAs(NestedRight.getDeclaredField("target"), NestedRight)
        then: "one generic tracked"
        res.genericAsString("K") == 'String'

        when: "nested declaration with non equal left"
        context = GenericsResolver.resolve(NestedLeft)
        res = context.fieldTypeAs(NestedLeft.getDeclaredField("target"), NestedLeft)
        then: "one generic tracked"
        res.genericAsString("K") == 'String'

        when: "known generic is a wildcard"
        context = GenericsResolver.resolve(Wildcard)
        res = context.fieldTypeAs(Wildcard.getDeclaredField("target"), Wildcard)
        then: "one generic tracked"
        res.genericAsString("K") == 'String'

        when: "transitive declaration with wildcard"
        context = GenericsResolver.resolve(WildcardDeclaration)
        res = context.fieldTypeAs(WildcardDeclaration.getDeclaredField("target"), WildcardDeclaration)
        then: "one generic tracked"
        res.genericAsString("B") == 'String'
        res.genericAsString("A") == 'Object'

        when: "transitive declaration with wildcard 2"
        context = GenericsResolver.resolve(WildcardDeclaration2)
        res = context.fieldTypeAs(WildcardDeclaration2.getDeclaredField("target"), WildcardDeclaration2)
        then: "wildcard generic aligned to base type"
        res.genericAsString("B") == 'String'
        res.genericAsString("A") == 'String'


        when: "array generic"
        context = GenericsResolver.resolve(ArrayLeft)
        res = context.fieldTypeAs(ArrayLeft.getDeclaredField("target"), ArrayLeft)
        then: "resovled"
        res.genericAsString("A") == 'String'

        when: "root scope contradicts with known generic"
        context = GenericsResolver.resolve(RootScopeContradiction)
        context.fieldTypeAs(RootScopeContradiction.getDeclaredField("target"), RootScopeContradiction)
        then: "error"
        def ex = thrown(GenericsTrackingException)
        ex.message == "Failed to track generics of RootScopeContradiction<T> from sub type Target<String>"
        ex.getCause().message == "Known generic T of Target<T> is not compatible with RootScopeContradiction hierarchy: String when required ? extends Integer"

        when: "root scope contradicts with known generic"
        context = GenericsResolver.resolve(RootScopeContradiction2)
        context.fieldTypeAs(RootScopeContradiction2.getDeclaredField("target"), RootScopeContradiction2)
        then: "error"
        ex = thrown(GenericsTrackingException)
        ex.message == "Failed to track generics of RootScopeContradiction2<T, K> from sub type Target<String>"
        ex.getCause().message == "Known generic T of Target<T> is not compatible with RootScopeContradiction2 hierarchy: String when required ? extends Integer"
    }

    def "No tracking required cases"() {

        when: "no generics in class"
        def res = GenericsTrackingUtils.track(Root, Base1, [:])
        then: "empty"
        res.isEmpty()

        when: "empty known generics"
        res = GenericsTrackingUtils.track(Base1, Lvl2Base1, [:])
        then: "empty"
        res.isEmpty()
    }
}

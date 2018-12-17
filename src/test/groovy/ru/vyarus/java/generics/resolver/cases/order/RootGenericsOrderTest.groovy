package ru.vyarus.java.generics.resolver.cases.order

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils
import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils
import ru.vyarus.java.generics.resolver.util.TypeLiteral
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 17.12.2018
 */
class RootGenericsOrderTest extends Specification {

    def "Check resolution with reverse declaration order"() {

        when: "reversed order"
        GenericsContext res = GenericsResolver.resolve(MyClass)
        then:
        TypeToStringUtils.toStringType(res.genericType("T"), EmptyGenericsMap.getInstance()) == "List<Object>"
        TypeToStringUtils.toStringType(res.genericType("D"), EmptyGenericsMap.getInstance()) == "Object"


        when: "mixed order"
        res = GenericsResolver.resolve(MixedOrderClass)
        then:
        TypeToStringUtils.toStringType(res.genericType("T"), EmptyGenericsMap.getInstance()) == "List<Collection<Object>>"
        TypeToStringUtils.toStringType(res.genericType("D"), EmptyGenericsMap.getInstance()) == "Collection<Object>"
        TypeToStringUtils.toStringType(res.genericType("P"), EmptyGenericsMap.getInstance()) == "Object"


        when: "multiple reversed orders"
        res = GenericsResolver.resolve(MyComplexClass)
        then:
        TypeToStringUtils.toStringType(res.genericType("T"), EmptyGenericsMap.getInstance()) == "List<Collection<Object>>"
        TypeToStringUtils.toStringType(res.genericType("D"), EmptyGenericsMap.getInstance()) == "Collection<Object>"
        TypeToStringUtils.toStringType(res.genericType("P"), EmptyGenericsMap.getInstance()) == "Object"


        when: "hard order mix"
        res = GenericsResolver.resolve(EnormousCase)
        then:
        TypeToStringUtils.toStringType(res.genericType("T"), EmptyGenericsMap.getInstance()) == "List<Object>"
        TypeToStringUtils.toStringType(res.genericType("P"), EmptyGenericsMap.getInstance()) == "Collection<List<Object>>"
        TypeToStringUtils.toStringType(res.genericType("K"), EmptyGenericsMap.getInstance()) == "Object"
    }

    def "Check direct generics resolution with reverse order"() {

        when: "reversed order"
        def res = GenericsResolutionUtils.resolveDirectRawGenerics(MyClass)
        then:
        res.keySet() as List == ["T", "D"] as List
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<Object>"
        TypeToStringUtils.toStringType(res["D"], EmptyGenericsMap.getInstance()) == "Object"


        when: "mixed order"
        res = GenericsResolutionUtils.resolveDirectRawGenerics(MixedOrderClass)
        then:
        res.keySet() as List == ["T", "P", "D"] as List
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<Collection<Object>>"
        TypeToStringUtils.toStringType(res["D"], EmptyGenericsMap.getInstance()) == "Collection<Object>"
        TypeToStringUtils.toStringType(res["P"], EmptyGenericsMap.getInstance()) == "Object"


        when: "multiple reversed orders"
        res = GenericsResolutionUtils.resolveDirectRawGenerics(MyComplexClass)
        then:
        res.keySet() as List == ["T", "D", "P"] as List
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<Collection<Object>>"
        TypeToStringUtils.toStringType(res["D"], EmptyGenericsMap.getInstance()) == "Collection<Object>"
        TypeToStringUtils.toStringType(res["P"], EmptyGenericsMap.getInstance()) == "Object"


        when: "hard order mix"
        res = GenericsResolutionUtils.resolveDirectRawGenerics(EnormousCase)
        then:
        res.keySet() as List == ["T", "K", "P"] as List
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<Object>"
        TypeToStringUtils.toStringType(res["P"], EmptyGenericsMap.getInstance()) == "Collection<List<Object>>"
        TypeToStringUtils.toStringType(res["K"], EmptyGenericsMap.getInstance()) == "Object"
    }

    def "Check generics tracking with reverse order"() {

        when: "reversed order"
        def known = new LinkedHashMap<>()
        known.put("K", String)
        def res = GenericsTrackingUtils.track(MyClass, Base, known)
        then:
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<String>"
        TypeToStringUtils.toStringType(res["D"], EmptyGenericsMap.getInstance()) == "String"

        when: "mixed order"
        known = new LinkedHashMap<>()
        known.put("K", new TypeLiteral<Set<String>>(){}.getType())
        res = GenericsTrackingUtils.track(MixedOrderClass, Base, known)
        then:
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<Set<String>>"
        TypeToStringUtils.toStringType(res["D"], EmptyGenericsMap.getInstance()) == "Set<String>"
        TypeToStringUtils.toStringType(res["P"], EmptyGenericsMap.getInstance()) == "String"


        when: "multiple reversed orders"
        known = new LinkedHashMap<>()
        known.put("K", new TypeLiteral<Set<String>>(){}.getType())
        res = GenericsTrackingUtils.track(MyComplexClass, Base, known)
        then:
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<Set<String>>"
        TypeToStringUtils.toStringType(res["D"], EmptyGenericsMap.getInstance()) == "Set<String>"
        TypeToStringUtils.toStringType(res["P"], EmptyGenericsMap.getInstance()) == "String"


        when: "hard order mix"
        known = new LinkedHashMap<>()
        known.put("K", new TypeLiteral<Set<List<String>>>(){}.getType())
        res = GenericsTrackingUtils.track(EnormousCase, Base, known)
        then:
        TypeToStringUtils.toStringType(res["T"], EmptyGenericsMap.getInstance()) == "List<String>"
        TypeToStringUtils.toStringType(res["P"], EmptyGenericsMap.getInstance()) == "Set<List<String>>"
        TypeToStringUtils.toStringType(res["K"], EmptyGenericsMap.getInstance()) == "String"
    }
}

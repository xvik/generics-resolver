package ru.vyarus.java.generics.resolver.inlying

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.inlying.support.DeclarationType
import ru.vyarus.java.generics.resolver.inlying.support.Err
import ru.vyarus.java.generics.resolver.inlying.support.RootType
import ru.vyarus.java.generics.resolver.inlying.support.SubType
import ru.vyarus.java.generics.resolver.support.Root
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 07.05.2018
 */
class InlyingGenericsResolutionTest extends Specification {

    def "Check inlying contexts resolution"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)

        when: "field context"
        def res = context.inlyingFieldType(DeclarationType.getDeclaredField("one"))
        then:
        res.generic("T") == Integer
        res.rootContext().currentClass() == DeclarationType.class

        when: "field context with interface"
        res = context.inlyingFieldType(DeclarationType.getDeclaredField("two"))
        then:
        res.generic("T") == Integer
        res.rootContext().currentClass() == DeclarationType.class

        when: "method return context"
        res = context.method(DeclarationType.getMethod("ret")).returnInlyingType()
        then:
        res.generic("T") == String
        res.rootContext().currentClass() == DeclarationType.class

        when: "method param context"
        res = context.method(DeclarationType.getMethod("param", SubType.class)).parameterInlyingType(0)
        then:
        res.generic("T") == Double
        res.rootContext().currentClass() == DeclarationType.class

        when: "wrong method param position"
        context.method(DeclarationType.getMethod("param", SubType.class)).parameterInlyingType(2)
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Can't request parameter 2 of method 'param' (DeclarationType) because it have only 1 parameters"

        when: "wrong field"
        context.inlyingFieldType(Err.getDeclaredField("wrongField"))
        then: "err"
        ex = thrown(IllegalArgumentException)
        ex.message == "Field 'wrongField' declaration type Err is not present in hierarchy of RootType"
    }

    def "Check inlying type without generics"() {

        setup: "prepare base type context"
        GenericsContext context = GenericsResolver.resolve(RootType)

        when: "field without generics"
        def res = context.inlyingFieldType(RootType.getDeclaredField("nogen"))
        then:
        res.rootContext().currentClass() == RootType.class
    }
}

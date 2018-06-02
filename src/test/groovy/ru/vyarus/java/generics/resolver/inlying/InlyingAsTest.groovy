package ru.vyarus.java.generics.resolver.inlying

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.TypeGenericsContext
import ru.vyarus.java.generics.resolver.inlying.support.*
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 08.05.2018
 */
class InlyingAsTest extends Specification {

    def "Check inlying contexts resolution"() {

        setup: "prepare base type context"
        TypeGenericsContext context = GenericsResolver.resolve(RootType)

        when: "field context"
        def res = context.fieldTypeAs(DeclarationType.getDeclaredField("one"), SubTypeExt)
        then:
        res.inlying
        res.generic("K") == Integer.class
        res.type(SubType).generic("T") == Integer
        res.rootContext().currentClass() == DeclarationType

        when: "field context with interface"
        res = context.fieldTypeAs(DeclarationType.getDeclaredField("two"), BaseIfaceImpl)
        then:
        res.generic("K") == Integer.class
        res.type(BaseIface).generic("T") == Integer
        res.rootContext().currentClass() == DeclarationType

        when: "method return context"
        res = context.method(DeclarationType.getMethod("ret")).returnTypeAs(SubTypeExt)
        then:
        res.generic("K") == String.class
        res.type(SubType).generic("T") == String
        res.rootContext().currentClass() == DeclarationType

        when: "method param context"
        res = context.method(DeclarationType.getMethod("param", SubType.class)).parameterTypeAs(0, SubTypeExt)
        then:
        res.generic("K") == Double
        res.type(SubType).generic("T") == Double
        res.rootContext().currentClass() == DeclarationType

        when: "wrong method param position"
        context.method(DeclarationType.getMethod("param", SubType.class)).parameterTypeAs(2, SubTypeExt)
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Can't request parameter 2 of method 'void param(SubType<Double>)' (DeclarationType) because it have only 1 parameters"

        when: "wrong field"
        context.fieldTypeAs(Err.getDeclaredField("wrongField"), SubTypeExt)
        then: "err"
        ex = thrown(IllegalArgumentException)
        ex.message == "Field 'wrongField' declaration type Err is not present in hierarchy of RootType"

        when: "incompatible type"
        context.fieldTypeAs(DeclarationType.getDeclaredField("one"), RootType)
        then: "err"
        ex = thrown(IllegalArgumentException)
        ex.message == "Requested type RootType is not a subtype of SubType"
    }

    def "Check inlying type without generics"() {

        setup: "prepare base type context"
        TypeGenericsContext context = GenericsResolver.resolve(RootType)

        when: "field without generics"
        def res = context.fieldTypeAs(RootType.getDeclaredField("nogen"), NoGenericTypeExt)
        then:
        res.rootContext().currentClass() == RootType.class
    }
}

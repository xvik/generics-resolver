package ru.vyarus.java.generics.resolver.cases.doc

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 02.06.2018
 */
class AllGenericsDocTest extends Specification {

    def "Check documentation correctness"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Root)
        println(context)

        then: "no root generics"
        context.genericsMap().isEmpty()

        when: "Outer class context"
        context = context.type(Outer.class)
        println(context)
        then: "correct Outer generics"
        context.genericsMap() == ["A": String, "B": Integer, "C": Long];

        when: "constructor context"
        context = context.constructor(Outer.getConstructor(Object))
        println(context)
        then:
        context.genericsMap() == ["A": String, "B": Integer, "C": Long]
        context.constructorGenericsMap() == ["C": Object]
        context.visibleGenericsMap() == ["A": String, "B": Integer, "C": Object]

        when: "method generics"
        context = context.method(Outer.getMethod("doSmth"))
        println(context)
        then:
        context.genericsMap() == ["A": String, "B": Integer, "C": Long]
        context.methodGenericsMap() == ["A": Object]
        context.visibleGenericsMap() == ["A": Object, "B": Integer, "C": Long]

        when: "field context generics"
        context = context.fieldType(Root.getDeclaredField("field1"))
        println(context)
        then:
        context.genericsMap() == ["A": Boolean, "T": String]
        context.ownerGenericsMap() == ["B": Integer, "C": Long]
        context.visibleGenericsMap() == ["A": Boolean, "T": String, "B": Integer, "C": Long]

        when: "explicit field context generics"
        context = context.rootContext().fieldType(Root.getDeclaredField("field2"))
        println(context)
        then:
        context.genericsMap() == ["A": Boolean, "T": String]
        context.ownerGenericsMap() == ["B": String, "C": String]
        context.visibleGenericsMap() == ["A": Boolean, "T": String, "B": String, "C": String]

        when: "inlying context usage"
        context = context.method(Outer.Inner.getMethod("doSmth2"))
        println(context)
        then:
        context.genericsMap() == ["A": Boolean, "T": String]
        context.ownerGenericsMap() == ["B": String, "C": String]
        context.methodGenericsMap() == ["B": Object]
        context.visibleGenericsMap() == ["A": Boolean, "T": String, "B": Object, "C": String]
    }
}
package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.ConstructorGenerics
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 25.05.2018
 */
class GenericsAccessorsTest extends Specification {

    def "Check generics scoping"() {

        when: "outer class"
        def context = GenericsResolver.resolve(Root).type(Outer)
        then:
        context.genericsMap() == ["A": String, "B": Integer, "C": Long]
        context.visibleGenericsMap() == context.genericsMap()
        context.ownerGenericsMap().isEmpty()
        context.rootContext() == null
        context.ownerClass() == null

        when: "field type context (inner class)"
        context = context.fieldType(Root.getDeclaredField('field'))
        then:
        context.genericsMap() == ["A": Double, "T": Comparable]
        context.ownerGenericsMap() == ["B": Integer, "C": Long]
        context.visibleGenericsMap() == ["A": Double, "T": Comparable, "B": Integer, "C": Long]
        context.rootContext() != null
        context.ownerClass() == Outer

        when: "method context"
        context = context.method(Outer.Inner.getDeclaredMethod('doSmth'))
        then:
        context.genericsMap() == ["A": Double, "T": Comparable]
        context.ownerGenericsMap() == ["B": Integer, "C": Long]
        context.methodGenericsMap() == ["P": Object]
        context.visibleGenericsMap() == ["A": Double, "T": Comparable, "B": Integer, "C": Long, "P": Object]
        context.rootContext() != null
        context.ownerClass() == Outer

        when: "method hiding outer class generic context"
        context = context.method(Outer.Inner.getDeclaredMethod('doSmth2'))
        then:
        context.genericsMap() == ["A": Double, "T": Comparable]
        context.ownerGenericsMap() == ["B": Integer, "C": Long]
        context.methodGenericsMap() == ["B": Object]
        context.visibleGenericsMap() == ["A": Double, "T": Comparable, "B": Object, "C": Long]
        context.rootContext() != null
        context.ownerClass() == Outer
    }

    def "Check constructor generics"() {

        when: "constructor context"
        def context = GenericsResolver.resolve(ConstructorGenerics).constructor(ConstructorGenerics.getConstructor(Object))
        then:
        context.genericsMap() == ["P": Object]
        context.constructorGenericsMap() == ["T": Object]
        context.visibleGenericsMap() == ["P": Object, "T": Object]

        when: "ctor context with name clash"
        context = context.constructor(ConstructorGenerics.getConstructor(Comparable))
        then:
        context.genericsMap() == ["P": Object] // generic is not visible, but it belongs to type and remain for better consistency
        context.constructorGenericsMap() == ["P": Comparable]
        context.visibleGenericsMap() == ["P": Comparable]
    }

    def "Check to string cases"() {

        when: "outer class"
        def context = GenericsResolver.resolve(Root).type(Outer)
        then:
        toString(context) == """class Root
  extends Outer<String, Integer, Long>    <-- current
"""

        when: "field type context (inner class)"
        context = context.fieldType(Root.getDeclaredField('field'))
        then:
        toString(context) == """class Outer<Object, Integer, Long>.Inner<Double, Comparable>  resolved in context of Root    <-- current
"""

        when: "method context"
        context = context.method(Outer.Inner.getDeclaredMethod('doSmth'))
        then:
        toString(context) == """class Outer<Object, Integer, Long>.Inner<Double, Comparable>  resolved in context of Root
  Object doSmth()    <-- current
"""

        when: "method hiding outer class generic context"
        context = context.method(Outer.Inner.getDeclaredMethod('doSmth2'))
        then:
        // generic B of outer is not visible, but here it shown as actual value (universal logic)
        toString(context) == """class Outer<Object, Integer, Long>.Inner<Double, Comparable>  resolved in context of Root
  Object doSmth2()    <-- current
"""

        when: "constructor"
        context = GenericsResolver.resolve(ConstructorGenerics).constructor(ConstructorGenerics.getConstructor(Object))
        then:
        toString(context) == """class ConstructorGenerics
  ConstructorGenerics(Object)    <-- current
"""

        when: "constructor 2"
        context = context.constructor(ConstructorGenerics.getConstructor(Comparable))
        then:
        toString(context) == """class ConstructorGenerics
  ConstructorGenerics(Comparable)    <-- current
"""
    }

    private toString(GenericsContext context) {
        return context.toString().replace("\r", "")
    }

    static class Root extends Outer<String, Integer, Long> {

        Inner<Double, Comparable> field
    }

    static class Outer<A, B, C> {

        // hide A outer generic
        class Inner<A, T> {

            // introduce method generics
            public <P> P doSmth() {}

            // method hides outer generic B
            public <B> B doSmth2() {}
        }
    }
}

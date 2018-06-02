package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.05.2018
 */
class FieldShortcutsTest extends Specification {

    def "Check field shortcuts"() {

        setup:
        GenericsContext context = GenericsResolver.resolve(Root)
        def field = Root.getField("field")

        expect:
        TypeToStringUtils.toStringType(context.resolveFieldType(field), [:]) == "List<Integer>"
        context.resolveFieldClass(field) == List
        context.resolveFieldGenerics(field) == [Integer]
        context.resolveFieldGeneric(field) == Integer
    }

    def "Check wrong field resolution"() {

        setup:
        GenericsContext context = GenericsResolver.resolve(Root)
        def field = Root.getField("field")

        when: "resolve type on wrong class"
        def res = context.resolveClass(field.getGenericType())
        then: 'context auto switched'
        res == List
    }

    def "Check not in hierarchy"() {

        setup:
        GenericsContext context = GenericsResolver.resolve(Root, Base)
        def field = Root.getField("field")

        when: 'accessing field of ignored class'
        context.resolveFieldClass(field)
        then:
        thrown(IllegalArgumentException)
    }

    static class Base<T> {

        public T field
    }

    static class Root extends Base<List<Integer>> {

    }
}
package ru.vyarus.java.generics.resolver.cases.primitives

import ru.vyarus.java.generics.resolver.GenericsResolver
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 16.01.2020
 */
class PrimitivesResolutionTest extends Specification {

    def "Check primitive types resolution"() {
        def context = GenericsResolver.resolve(Sample)

        expect: "primitive types not wrapped"
        context.resolveClass(int) == int
        context.resolveType(int) == int
        context.resolveTypeGenerics(int) == []
        context.resolveGenericOf(int) == null
        context.resolveGenericsOf(int) == []
        context.resolveFieldClass(Sample.getDeclaredField("field")) == int
        context.resolveFieldType(Sample.getDeclaredField("field")) == int
        context.resolveFieldGeneric(Sample.getDeclaredField("field")) == null
        context.resolveFieldGenerics(Sample.getDeclaredField("field")) == []
    }

    static class Sample {

        int field
    }
}

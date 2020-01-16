package ru.vyarus.java.generics.resolver.cases.primitives

import ru.vyarus.java.generics.resolver.GenericsResolver
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 16.01.2020
 */
class PrimitiveMethodParamTest extends Specification {

    def "Check primitive resolution"() {
        def context = GenericsResolver.resolve(Sample).method(Sample.getDeclaredMethod('some', int, List, short))

        expect: "primitives preserved"
        context.resolveParametersTypes() == [int, List, short]
        context.resolveParameterType(0) == int
        context.resolveParameters() == [int, List, short]
        context.resolveReturnClass() == int
        context.resolveReturnType() == int
        context.resolveReturnTypeGeneric() == null
        context.resolveReturnTypeGenerics() == []
        context.resolveReturnTypeGeneric() == null

        and: "to string shows primitives"
        context.toStringMethod() == "int some(int, List, short)"

        and: "contexts use wrappers"
        context.parameterType(0).currentClass() == Integer
        context.returnType().currentClass() == Integer
    }

    static class Sample {
        int some(int one, List two, short three) {
            return 0;
        }
    }
}

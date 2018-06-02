package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 16.12.2015
 */
class DependantRawTypesTest extends Specification {

    def "Check dependant raw types"() {

        when: "dependant root generics declared"
        GenericsContext context = GenericsResolver.resolve(SelfPairProperties)

        then: "correctly resolved"
        context.genericsMap() == ["F": Object, "S": Object]
        context.type(PairProperties).genericsMap() == ["F": Object, "S": Object]
        context.method(PairProperties.getMethod('firstAndSecond', Object, Object)).resolveParameters() == [Object, Object]
    }

    static class SelfPairProperties<F, S extends F> extends PairProperties<F, S> {
    }

    static class PairProperties<F, S> {
        void firstAndSecond(F first, S second) {
        }
    }
}

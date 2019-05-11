package ru.vyarus.java.generics.resolver.cases.cycle

import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils
import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 10.05.2019
 */
class CycledGenericsTest extends Specification{

    def "Check cycled generic resolution"() {

        when: "detecting raw generic for cycled type"
        def res = GenericsResolutionUtils.resolveDirectRawGenerics(CycledGeneric)
        then:
        res.size() == 1
        res["T"] == CycledGeneric

    }

    def "Check cycled types comparison"() {

        when: "match cycled types"
        def res = TypeUtils.isMoreSpecific(CycledGeneric, CycledGeneric)
        then:
        !res
    }
}

package ru.vyarus.java.generics.resolver.cases


import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 29.09.2020
 */
class EnumCaseTest extends Specification {

    def "Check enum comparison"() {

        expect:
        TypeUtils.isCompatible(Enum, Enm)
        TypeUtils.isAssignable(Enm, Enum)
        TypeUtils.isMoreSpecific(Enm, Enum)
    }

    static enum Enm {
        ONE
    }
}
package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import ru.vyarus.java.generics.resolver.support.Base2
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import ru.vyarus.java.generics.resolver.util.map.EmptyGenericsMap
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap
import spock.lang.Specification

import java.lang.reflect.Type

/**
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
class SpecialMapsTest extends Specification {

    def "Check empty map behaviour"() {

        when: "adding to empty map"
        EmptyGenericsMap.getInstance().put("d", null)
        then:
        thrown(UnsupportedOperationException)

        when: "adding to empty map 2"
        EmptyGenericsMap.getInstance().putAll(new HashMap<String, Type>())
        then:
        thrown(UnsupportedOperationException)
    }

    def "Check ignoring map behaviour"() {

        when: "verification case"
        TypeToStringUtils.toStringType(new ParameterizedTypeImpl(Base2, Base2.getTypeParameters()), ["K": String])
        then: "error"
        def ex = thrown(UnknownGenericException)
        ex.genericName == "P"

        when: "using ignoring map"
        def res = TypeToStringUtils.toStringType(new ParameterizedTypeImpl(Base2, Base2.getTypeParameters()),
                new IgnoreGenericsMap(["K": String]))
        then:
        res == "Base2<String, Object>"

        when: "adding to map"
        IgnoreGenericsMap.getInstance().put("d", null)
        then:
        thrown(UnsupportedOperationException)

        when: "adding to map 2"
        IgnoreGenericsMap.getInstance().putAll(new HashMap<String, Type>())
        then:
        thrown(UnsupportedOperationException)
    }

    def "Check printable generic names"() {

        when: "verification case"
        TypeToStringUtils.toStringType(new ParameterizedTypeImpl(Base2, Base2.getTypeParameters()), ["K": String])
        then: "error"
        def ex = thrown(UnknownGenericException)
        ex.genericName == "P"

        when: "using ignoring map"
        def res = TypeToStringUtils.toStringType(new ParameterizedTypeImpl(Base2, Base2.getTypeParameters()),
                new PrintableGenericsMap(["K": String]))
        then:
        res == "Base2<String, P>"
    }
}
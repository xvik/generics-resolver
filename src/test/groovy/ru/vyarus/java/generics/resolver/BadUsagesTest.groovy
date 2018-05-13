package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov 
 * @since 27.06.2015
 */
class BadUsagesTest extends Specification {

    def "Check generic resolution on wrong class"() {

        when: "resolving type on wrong class"
        GenericsResolver.resolve(Root).resolveClass(Lvl2Base1.getMethod("doSomth2").getGenericReturnType())

        then: "generic not found"
        def th = thrown(UnknownGenericException)
        th.genericName == "I"

        when: "to string type on wrong class"
        GenericsResolver.resolve(Root).toStringType(Lvl2Base1.getMethod("doSomth2").getGenericReturnType())

        then: "generic not found"
        th = thrown(UnknownGenericException)
        th.genericName == "I"
    }
}
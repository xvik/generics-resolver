package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Model
import ru.vyarus.java.generics.resolver.support.Root
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov 
 * @since 27.06.2015
 */
class BadUsagesTest extends Specification {

    def "Check generic resolution on wrong class"() {

        when: "resolving type on wrong class"
        def res = GenericsResolver.resolve(Root).resolveClass(Lvl2Base1.getMethod("doSomth2").getGenericReturnType())

        then: "context switched automatically"
        res == Model

        when: "to string type on wrong class"
        res = GenericsResolver.resolve(Root).toStringType(Lvl2Base1.getMethod("doSomth2").getGenericReturnType())

        then: "context switched automatically"
        res == "Model"
    }
}
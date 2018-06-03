package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Model
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 03.06.2018
 */
class GenericsResolutionTest extends Specification {

    def "Check direct generics resolution"() {

        when: "resolve raw generics"
        def res = GenericsResolutionUtils.resolve(Root)
        then:
        res.size() == 8
        res[Base1] == ["T": Model]

        when: "resolve class with generics"
        res = GenericsResolutionUtils.resolve(Base1)
        then:
        res[Base1] == ["T": Object]

        when: "resolving class with ignore"
        res = GenericsResolutionUtils.resolve(Root, Base1)
        then:
        res[Base1] == null
    }
}
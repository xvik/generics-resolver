package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.BeanRoot
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.support.noclash.NoClashRoot
import ru.vyarus.java.generics.resolver.support.noclash.NoClashSub1
import ru.vyarus.java.generics.resolver.support.noclash.NoClashSub2
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import spock.lang.Specification

import java.util.concurrent.Callable


/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2014
 */
class FailTests extends Specification {

    def "Check fails"() {

        when: "accessing type not in hierarchy"
        GenericsResolver.resolve(Root).type(BeanRoot)
        then: "fail"
        thrown(IllegalArgumentException)
    }

    def "No clash on same not parametrized interface"() {

        when: "resolving type with duplicate Runnable interface in hierarchy"
        GenericsContext context = GenericsResolver.resolve(NoClashRoot)
        then: "context is empty, because no generics info available in hierarchy"
        context.genericsInfo.composingTypes == [NoClashRoot, NoClashSub1, NoClashSub2, Runnable, Callable] as Set
    }

    def "Check access with wrong name"() {

        when: "trying to access generic with bad name"
        GenericsResolver.resolve(Root).type(Base1).generic("K")
        then: "fail"
        thrown(UnknownGenericException)

    }

    def "Check type resolution fail"() {
        when: "trying to access generic with bad name"
        GenericsResolver.resolve(Root).resolveType(Lvl2Base1.getMethod("doSomth2").getGenericReturnType())
        then: "fail"
        def ex = thrown(UnknownGenericException)
        ex.genericName == "I"
        ex.contextType == Root
    }
}
package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.BeanBase
import ru.vyarus.java.generics.resolver.support.BeanRoot
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Lvl2BeanBase
import ru.vyarus.java.generics.resolver.support.clash.ClashRoot
import spock.lang.Specification

import java.util.concurrent.Callable

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2014
 */
class IgnoreTest extends Specification {

    def "Check ignore usage"() {

        when: "using ignore to overcome interface clash"
        GenericsContext context = GenericsResolver.resolve(ClashRoot, Callable)
        then: "without callable context correctly resolved"
        !context.genericsInfo.composingTypes.contains(Callable)

        when: "using ignore to limit class resolution depth"
        context = GenericsResolver.resolve(BeanRoot, Lvl2BeanBase)
        then: "only root types resolved"
        context.genericsInfo.composingTypes == [BeanRoot, BeanBase, Lvl2Base1] as Set
    }
}
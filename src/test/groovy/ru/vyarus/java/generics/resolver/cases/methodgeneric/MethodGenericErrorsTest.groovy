package ru.vyarus.java.generics.resolver.cases.methodgeneric

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.MethodGenericCase
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.SubMethodGenericCase
import ru.vyarus.java.generics.resolver.context.MethodGenericsContext
import spock.lang.Specification

import java.lang.reflect.Method


/**
 * @author Vyacheslav Rusakov 
 * @since 27.06.2015
 */
class MethodGenericErrorsTest extends Specification {

    def "Check error on bad type"() {

        when: "check incorrect method context creation"

        def method = MethodGenericCase.getMethod("testSub", Class, Object)
        new MethodGenericsContext(GenericsResolver.resolve(MethodGenericCase).getGenericsInfo(),
        MethodGenericCase, method);

        then: "type incompatible"
        thrown(IllegalArgumentException)

        when: "navigating to method from different type"
        MethodGenericsContext context = GenericsResolver.resolve(MethodGenericCase)
                .method(method);

        then: "ok"
        context.currentClass() == SubMethodGenericCase
        context.currentMethod() == method
    }
}
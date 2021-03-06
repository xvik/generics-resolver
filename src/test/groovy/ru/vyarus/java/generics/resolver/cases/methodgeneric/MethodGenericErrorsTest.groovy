package ru.vyarus.java.generics.resolver.cases.methodgeneric

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.Err
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.MethodGenericCase
import ru.vyarus.java.generics.resolver.cases.methodgeneric.support.SubMethodGenericCase
import ru.vyarus.java.generics.resolver.context.GenericsContext
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov 
 * @since 27.06.2015
 */
class MethodGenericErrorsTest extends Specification {

    def "Check error on bad type"() {

        setup:
        GenericsContext context = GenericsResolver.resolve(MethodGenericCase)

        when: "navigating to method from different type"
        context.method(Err.getMethod("errMeth"))
        then: "type incompatible"
        def ex = thrown(IllegalArgumentException)
        ex.message.replace('\r', '') == """Method 'void errMeth()' declaration type Err is not present in current hierarchy:
class MethodGenericCase
  extends SubMethodGenericCase<Cloneable>
"""

        when: "navigating to method from different type"
        def method = MethodGenericCase.getMethod("testSub", Class, Object)
        def res = context.method(method);

        then: "ok"
        res.currentClass() == SubMethodGenericCase
        res.currentMethod() == method
    }
}
package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.tostring.Base
import ru.vyarus.java.generics.resolver.support.tostring.GenerifiedInterface
import ru.vyarus.java.generics.resolver.support.tostring.TSBase
import ru.vyarus.java.generics.resolver.support.tostring.TSRoot
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2014
 */
class ToStringTest extends Specification {
    def "Complex to string"() {

        when:
        GenericsContext context = GenericsResolver.resolve(TSRoot).type(TSBase)
        then:
        context.genericsAsString() == ["Model", "ArrayList<SType<Model, Model[]>>"]

        then:
        context.toStringType(TSBase.getMethod("doSomth").getGenericReturnType()) == "Model[]"
    }

    def "Complex to string 2"() {

        when: "resolving all types of interface generics"
        GenericsContext context = GenericsResolver.resolve(Base).type(GenerifiedInterface)
        then: "everything is ok"
        context.genericsAsString() == ['Integer', 'String[]', 'List<String>', 'List<Set<String>>']
    }
}
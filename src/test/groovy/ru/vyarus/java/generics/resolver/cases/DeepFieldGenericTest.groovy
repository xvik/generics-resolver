package ru.vyarus.java.generics.resolver.cases

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import spock.lang.Specification

import java.lang.reflect.Type

/**
 * @author Vyacheslav Rusakov
 * @since 28.03.2020
 */
class DeepFieldGenericTest extends Specification {

    def "Check deep generic resolution"() {

        when: "resolving deep generic"
        GenericsContext context = GenericsResolver
                // SomeBean
                .resolve(SomeBean)
                // List<AnotherBean<List<String>>>
                .fieldType(SomeBean.getDeclaredField("listInSomeBean"))

        // AnotherBean<List<String>>
        Type type = context.genericType(0)
        // List<String>
        type = context.resolveTypeGenerics(type)[0]
        // String
        Class res = context.resolveGenericOf(type)

        then: "correct resolution"
        res == String
    }

    static class SomeBean {
        private List<AnotherBean<List<String>>> listInSomeBean;
    }

    static class AnotherBean<T> {

        private List<T> listWithGeneric
    }
}

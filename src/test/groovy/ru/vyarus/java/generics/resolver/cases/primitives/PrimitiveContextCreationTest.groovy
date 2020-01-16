package ru.vyarus.java.generics.resolver.cases.primitives

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.GenericsInfo
import spock.lang.Specification

import java.lang.reflect.Type

/**
 * @author Vyacheslav Rusakov
 * @since 16.01.2020
 */
class PrimitiveContextCreationTest extends Specification {

    def "Check primitive context creation deny"() {

        when: "directly creating context for primitive"
        new GenericsContext(new GenericsInfo(int, new LinkedHashMap<Class<?>, LinkedHashMap<String, Type>>()), int)
        then: "denied"
        def ex = thrown(IllegalArgumentException)
        ex.message == "Primitive type int can't be used for generics context building"
    }
}

package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.TypeGenericsContext
import ru.vyarus.java.generics.resolver.support.*
import spock.lang.Specification

import java.lang.reflect.ParameterizedType

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2014
 */
class CurrentIntrospectionTest extends Specification {
    def "Check current introspection on interface"() {

        when:
        TypeGenericsContext context = GenericsResolver.resolve(Root).type(ComplexGenerics)

        then:
        context.genericsInfo
        context.currentClass() == ComplexGenerics
        context.generics() == [Model, List]
        context.generic(0) == Model
        context.generic("T") == Model
        context.generic(1) == List
        context.generic("K") == List
        context.genericsAsString() == ["Model", "List<Model>"]
        context.genericAsString(0) == "Model"
        context.genericAsString("T") == "Model"
        context.genericAsString(1) == "List<Model>"
        context.genericAsString("K") == "List<Model>"
        context.genericTypes()[0] == Model
        context.genericTypes()[1] instanceof ParameterizedType
        context.genericType(0) == Model
        context.genericType("T") == Model
        context.genericType(1) instanceof ParameterizedType
        context.genericType("K") instanceof ParameterizedType
        context.genericsMap().keySet() == ["T", "K"] as Set

        when: "array generic"
        context = context.type(ComplexGenerics2)

        then:
        context.generics() == [Model[]]
        context.generic(0) == Model[]
        context.genericsAsString() == ["Model[]"]
        context.genericTypes() == [Model[]]
        context.genericsMap().keySet() == ["T"] as Set
    }

    def "Check current introspection on bean"() {

        when: "introspecting base class"
        TypeGenericsContext context = GenericsResolver.resolve(BeanRoot).type(Lvl2BeanBase)

        then:
        context.genericsInfo
        context.currentClass() == Lvl2BeanBase
        context.generics() == [Model]
        context.generic(0) == Model
        context.genericsAsString() == ["Model"]
        context.genericAsString(0) == "Model"
        context.genericTypes() == [Model]
        context.genericType(0) == Model
        context.genericsMap().keySet() == ["I"] as Set

        when: "introspecting interface, implemented by base class"
        context = context.type(Lvl2Base1)

        then:
        context.currentClass() == Lvl2Base1
        context.generics() == [Model]
        context.generic(0) == Model
        context.genericsAsString() == ["Model"]
        context.genericAsString(0) == "Model"
        context.genericTypes() == [Model]
        context.genericType(0) == Model
        context.genericsMap().keySet() == ["I"] as Set
    }
}
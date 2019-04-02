package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsInfo
import ru.vyarus.java.generics.resolver.context.GenericsInfoFactory
import ru.vyarus.java.generics.resolver.support.*
import ru.vyarus.java.generics.resolver.support.array.ArBaseLvl2
import ru.vyarus.java.generics.resolver.support.array.ArRoot
import spock.lang.Specification

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType

/**
 * @author Vyacheslav Rusakov 
 * @since 16.10.2014
 */
class GenericsInfoFactoryTest extends Specification {

    def "Check generics resolution"() {

        when: "analyzing finders hierarchy"
        GenericsInfo info = GenericsInfoFactory.create(Root)
        then: "correct generic values resolved"
        info.rootClass == Root
        info.composingTypes.size() == 8
        info.getTypeGenerics(Base1) == ['T': Model]
        info.getTypeGenerics(Base2) == ['K': Model, 'P': OtherModel]
        info.getTypeGenerics(Lvl2Base1) == ['I': Model]
        info.getTypeGenerics(Lvl2Base2) == ['J': Model]
        info.getTypeGenerics(Lvl2Base3) == ['R': Model]
        info.getTypeGenerics(ComplexGenerics)['T'] == Model
        info.getTypeGenerics(ComplexGenerics)['K'] instanceof ParameterizedType
        ((ParameterizedType) info.getTypeGenerics(ComplexGenerics)['K']).getRawType() == List
        info.getTypeGenerics(ComplexGenerics2)['T'] == Model[]

        when: "analyzing bean finders hierarchy"
        info = GenericsInfoFactory.create(BeanRoot)
        then: "correct generic values resolved"
        info.rootClass == BeanRoot
        info.composingTypes.size() == 4
        info.getTypeGenerics(BeanBase) == ['T': Model]
        info.getTypeGenerics(Lvl2BeanBase) == ['I': Model]
        info.getTypeGenerics(Lvl2Base1) == ['I': Model]

        when: "check duplicate resolution"
        def one = GenericsInfoFactory.create(BeanRoot)
        def two = GenericsInfoFactory.create(BeanRoot)
        then: "second resolution is cached"
        GenericsInfoFactory.cacheEnabled
        one == two
    }

    def "Check array generic resolution"() {

        when: "resolving bean with generic array"
        GenericsInfo info = GenericsInfoFactory.create(ArRoot)
        then: "resolved"
        info.getTypeGenerics(ArBaseLvl2)['T'] instanceof GenericArrayType
        (info.getTypeGenerics(ArBaseLvl2)['T'] as GenericArrayType).genericComponentType == param(List, Model)
    }
}

package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsInfo
import ru.vyarus.java.generics.resolver.context.GenericsInfoFactory
import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.support.Model
import ru.vyarus.java.generics.resolver.support.array.ArBase
import ru.vyarus.java.generics.resolver.support.array.ArBaseLvl2
import ru.vyarus.java.generics.resolver.support.array.ArRoot
import spock.lang.Specification

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.WildcardType


/**
 * @author Vyacheslav Rusakov 
 * @since 15.12.2014
 */
class ContainersTest extends Specification {

    def "Check containers correctness"() {

        when: "resolving types"
        GenericsInfo info = GenericsInfoFactory.create(ArRoot)
        GenericArrayType array = info.getTypeGenerics(ArBaseLvl2)['T']
        Class wildcardUpper = info.getTypeGenerics(ArBase)['K']
        WildcardType wildcardLower = info.getTypeGenerics(ArBase)['J']
        ParameterizedType parametrized = info.getTypeGenerics(ArBaseLvl2)['K']

        then: "array wrapper valid"
        array instanceof GenericArrayTypeImpl
        array.genericComponentType.toString() == "List<Model>"
        array.toString() == "List<Model>[]"

        then: "upper wildcard solved to simple type"
        wildcardUpper == Model

        then: "lower wildcard valid"
        wildcardLower instanceof WildcardTypeImpl
        wildcardLower.getLowerBounds() == [Model]
        wildcardLower.getUpperBounds() == [Object]
        wildcardLower.toString() == "? super Model"

        then: "parametrized wrapper valid"
        parametrized instanceof ParameterizedTypeImpl
        parametrized.getRawType() == List
        parametrized.getActualTypeArguments() == [new ParameterizedTypeImpl(List, Model)]
        parametrized.getOwnerType() == null
        parametrized.toString() == "List<List<Model>>"
    }

    def "Check wildcard methods"() {

        WildcardType wildcardUpper = WildcardTypeImpl.upper(Model)
        WildcardTypeImpl wildcardLower = WildcardTypeImpl.lower(Model)
        
        expect: "upper wildcard valid"
        wildcardUpper instanceof WildcardTypeImpl
        wildcardUpper.getUpperBounds() == [Model]
        wildcardUpper.getLowerBounds().length == 0
        wildcardUpper.toString() == "? extends Model"

        and: "lower wildcard valid"
        wildcardLower instanceof WildcardTypeImpl
        wildcardLower.getLowerBounds() == [Model]
        wildcardLower.getUpperBounds() == [Object]
        wildcardLower.toString() == "? super Model"
    }
}
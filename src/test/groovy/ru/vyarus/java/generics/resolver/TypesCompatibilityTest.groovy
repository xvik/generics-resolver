package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils
import spock.lang.Specification

import java.lang.reflect.GenericArrayType
import java.lang.reflect.ParameterizedType
import java.lang.reflect.Type

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
class TypesCompatibilityTest extends Specification {

    def "Check types compatibility"() {

        expect:
        GenericsResolutionUtils.isCompatible(type1, type2) == res

        where:
        type1                          | type2                       | res
        String                         | Integer                     | false
        Object                         | Integer                     | true
        String                         | Object                      | true
        Base1                          | Root                        | true
        Root                           | Base1                       | true
        param(List, String)            | param(List, Integer)        | false
        param(List, Base1)             | param(List, Root)           | true
        param(List, Root)              | param(List, Base1)          | true
        array(String)                  | array(Integer)              | false
        array(Base1)                   | array(Root)                 | true
        array(Integer)                 | String                      | false
        param(List, String)            | param(ArrayList, Integer)   | false
        param(ArrayList, String)       | param(List, Integer)        | false
        param(ArrayList, Base1)        | param(List, Root)           | true
        array(param(List, String))     | array(param(List, Integer)) | false
        array(param(ArrayList, Base1)) | array(param(List, Root))    | true
        new String[0].class            | new Integer[0].class        | false
        new Base1[0].class             | new Root[0].class           | true

    }

    def "Check types comparison"() {
        expect:
        GenericsResolutionUtils.isMoreSpecific(type1, type2) == res

        where:
        type1                    | type2                    | res
        Base1                    | Root                     | false
        Root                     | Base1                    | true
        param(List, String)      | param(List, Object)      | true
        param(List, Base1)       | param(List, Root)        | false
        array(Base1)             | array(Root)              | false
        array(Root)              | array(Base1)             | true
        param(List, String)      | param(ArrayList, String) | false
        param(ArrayList, String) | param(List, String)      | true
        param(ArrayList, Object) | param(ArrayList, String) | false
        new Base1[0].class       | new Root[0].class        | false
        new Root[0].class        | new Base1[0].class       | true
    }

    def "Check type comparison failure"() {

        when: "compare incopatible types"
        GenericsResolutionUtils.isMoreSpecific(String, Integer)
        then: "err"
        def ex = thrown(IllegalArgumentException)
        ex.message == "Type String can't be compared to Integer because they are not compatible"
    }

    def "Check specific type resolution"() {

        expect:
        GenericsResolutionUtils.getMoreSpecificType(Base1, Root) == Root
    }

    ParameterizedType param(Class root, Type... types) {
        return new ParameterizedTypeImpl(root, types)
    }

    GenericArrayType array(Type type) {
        return new GenericArrayTypeImpl(type)
    }
}

package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import ru.vyarus.java.generics.resolver.util.TypeUtils
import ru.vyarus.java.generics.resolver.util.type.instance.GenericArrayInstanceType
import ru.vyarus.java.generics.resolver.util.type.instance.InstanceType
import ru.vyarus.java.generics.resolver.util.type.instance.ParameterizedInstanceType
import ru.vyarus.java.generics.resolver.util.type.instance.WildcardInstanceType
import spock.lang.Specification

import java.lang.reflect.GenericArrayType
import java.lang.reflect.Type

/**
 * @author Vyacheslav Rusakov
 * @since 27.03.2019
 */
class InstanceTypesTest extends Specification {

    def "Check instance type extraction"() {

        expect:
        TypeUtils.getInstanceType([inst] as Object[]) == res

        where:
        inst                   | res
        "some"                 | String
        12                     | Integer
        [1, 2, 3] as Integer[] | new GenericArrayTypeImpl(Integer)
        [1, 1.2] as Number[]   | new GenericArrayTypeImpl(WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number)))
    }

    def "Check list case"() {

        def list = [1, null, 1.2]
        when: "analyzing list"
        Type res = TypeUtils.getInstanceType(list);
        then:
        res instanceof InstanceType
        (res as InstanceType).with {
            getInstance() == list
            !hasMultipleInstances()
            getAllInstances() == [list] as Object[]
            !isCompleteType()
        }
        TypeToStringUtils.toStringType(res) == "ArrayList"

        when: "resolving inner type"
        Type inner = TypeUtils.getInstanceType(list.toArray())
        then:
        inner instanceof InstanceType
        (inner as InstanceType).with {
            getInstance() == 1
            hasMultipleInstances()
            getAllInstances() == [1, 1.2] as Object[]
            isCompleteType()
        }

        when: "correcting original type"
        (res as ParameterizedInstanceType).improveAccuracy(inner)
        then:
        (res as InstanceType).isCompleteType()
        TypeToStringUtils.toStringType(res) == "ArrayList<? extends Number & Comparable<Number>>"
    }

    def "Check null filtering"() {

        when: "all values are null"
        Type res = TypeUtils.getInstanceType(null, null, null)
        then: "object"
        res == Object

        when: 'all values in array are null'
        res = TypeUtils.getInstanceType([[null, null] as Integer[]] as Object[])
        then: "simple class"
        res == Integer[]

        when: "some values are null"
        res = TypeUtils.getInstanceType(1, null, 2, null)
        then: "nulls filtered"
        res instanceof InstanceType
        (res as InstanceType).getAllInstances() == [1, 2] as Object[]

        when: "all arrays are empty"
        res = TypeUtils.getInstanceType([] as  Integer[], [] as Double[])
        then: "array type returned"
        !(res instanceof InstanceType)
        res instanceof GenericArrayType
        !(((GenericArrayType)res).genericComponentType instanceof InstanceType)
        res.toString() == "? extends Number & Comparable<Number>[]"

        when: "all arrays are with nulls"
        res = TypeUtils.getInstanceType([null] as  Integer[], [null] as Double[])
        then: "array type returned"
        !(res instanceof InstanceType)
        res instanceof GenericArrayType
        !(((GenericArrayType)res).genericComponentType instanceof InstanceType)
        res.toString() == "? extends Number & Comparable<Number>[]"
    }

    def "Check parameterized type behaviour"() {

        when: "incorrect creation"
        new ParameterizedInstanceType(String)
        then: "error"
        def ex = thrown(IllegalArgumentException)
        ex.message == "No instances provided"

        when: "create simple type"
        def type = new ParameterizedInstanceType(String, "abc")
        then: "pure class instance type"
        type.isCompleteType()
        type.rawType == String
        type.ownerType == null
        type.actualTypeArguments.length == 0
        !type.hasMultipleInstances()
        type.instance == "abc"
        type.allInstances == ["abc"] as Object[]
        type.toString().matches("String \\(\\w+\\)")
        type.improvableType == type
        type.iterator().next() == type.instance

        when: "create improvable type"
        type = new ParameterizedInstanceType(List, ["abc"])
        then: "generalized type"
        !type.isCompleteType()
        type.rawType == List
        type.ownerType == null
        type.actualTypeArguments == [Object] as Type[]
        !type.hasMultipleInstances()
        type.instance == ["abc"]
        type.allInstances == [["abc"]] as Object[]
        type.toString().matches("List \\(\\w+\\)")

        when: "improve type"
        type.improveAccuracy(String)
        then:
        type.isCompleteType()
        type.actualTypeArguments == [String] as Type[]
        !type.isMoreSpecificGenerics(Object)
        type.toString().matches("List<String> \\(\\w+\\)")

        when: "try incorrect improvement"
        type.improveAccuracy(Object)
        then:
        ex = thrown(IllegalArgumentException)
        ex.message == "Provided generics for type List [Object] are less specific then current [String]"

        when: "try improvement with incorrect count"
        type.improveAccuracy(Object, Object)
        then:
        ex = thrown(IllegalArgumentException)
        ex.message == "Wrong generics count provided <Object, Object> in compare to current types <String>"

        when: "create from parameterizable type"
        type = new ParameterizedInstanceType(new ParameterizedTypeImpl(List, [String] as Type[], Comparable), ["123"])
        then:
        type.rawType == List
        type.ownerType == Comparable
        type.actualTypeArguments == [String] as Type[]
        !type.completeType
        type.toString().matches("Comparable\\.List<String> \\(\\w+\\)")
        type.equals(new ParameterizedInstanceType(new ParameterizedTypeImpl(List, [String] as Type[], Comparable), ["123"]))

        when: "type contains multiple generics"
        type = new ParameterizedInstanceType(String, "one", "two", "three")
        then:
        type.toString().matches("String \\(\\w+,...\\(3\\)\\)")

        when: "equals and hashcode cases"
        type = new ParameterizedInstanceType(String, "abc")
        def type2 = new ParameterizedInstanceType(String, "cde")
        then:
        type.hashCode() == type2.hashCode()
        type.equals(type2)
        !type.equals(List)
        type.equals(String)
        type.equals(new ParameterizedTypeImpl(String))
        !type.equals(new ParameterizedTypeImpl(List))
    }

    def "Check wildcard type behaviour"() {

        when: "no instances"
        new WildcardInstanceType([String] as Type[])
        then: "error"
        def ex = thrown(IllegalArgumentException)
        ex.message == "No instances provided"

        when: "no bounds"
        new WildcardInstanceType([] as Type[], "1")
        then: "error"
        ex = thrown(IllegalArgumentException)
        ex.message == "No upper bounds provided"

        when: "instance type not first"
        new WildcardInstanceType([String, new ParameterizedInstanceType(String, "12")] as Type[], "1")
        then: "error"
        ex = thrown(IllegalArgumentException)
        ex.message == "Only the first type could be an instance type but type 2 is class ru.vyarus.java.generics.resolver.util.type.instance.ParameterizedInstanceType (all types: String, String)"

        when: "no instance type inside"
        def type = new WildcardInstanceType([String, Comparable] as Type[], "asd")
        then:
        type.completeType
        !type.hasMultipleInstances()
        type.upperBounds == [String, Comparable] as Type[]
        type.lowerBounds.length == 0
        type.instance == "asd"
        type.iterator().next() == type.instance
        type.allInstances == ["asd"] as Object[]
        type.improvableType == null
        type.toString().matches("\\? extends String & Comparable \\(\\w+\\)")

        when: "instance type inside"
        def improvable = new ParameterizedInstanceType(String, "aad")
        type = new WildcardInstanceType([improvable, Comparable] as Type[], "asd")
        then:
        type.completeType
        !type.hasMultipleInstances()
        type.upperBounds == [new ParameterizedTypeImpl(String), Comparable] as Type[]
        type.lowerBounds.length == 0
        type.instance == "asd"
        type.iterator().next() == type.instance
        type.allInstances == ["asd"] as Object[]
        type.improvableType == improvable
        type.toString().matches("\\? extends String & Comparable \\(\\w+\\)")

        when: "multiple instances"
        type = new WildcardInstanceType([String, Comparable] as Type[], "asd", "aad", "ddd")
        then:
        type.hasMultipleInstances()
        type.lowerBounds.length == 0
        type.instance == "asd"
        type.allInstances == ["asd", "aad", "ddd"] as Object[]
        type.toString().matches("\\? extends String & Comparable \\(\\w+,...\\(3\\)\\)")

        when: "equals and hashcode cases"
        def type2 = new WildcardInstanceType([String, Comparable] as Type[], "asd")
        then:
        type.hashCode() == type2.hashCode()
        type.equals(type2)
        type.equals(WildcardTypeImpl.upper(String, Comparable))
        !type.equals(WildcardTypeImpl.upper(String))
    }


    def "Check array type behaviour"() {

        when: "no instances"
        new GenericArrayInstanceType(String)
        then: "error"
        def ex = thrown(IllegalArgumentException)
        ex.message == "No instances provided"

        when: "no instance type inside"
        def type = new GenericArrayInstanceType(String, "asd")
        then:
        type.completeType
        !type.hasMultipleInstances()
        type.genericComponentType == String
        type.instance == "asd"
        type.iterator().next() == type.instance
        type.allInstances == ["asd"] as Object[]
        type.improvableType == null
        type.toString().matches("String\\[] \\(\\w+\\)")

        when: "instance type inside"
        def improvable = new ParameterizedInstanceType(String, "aad")
        type = new GenericArrayInstanceType(improvable, "asd")
        then:
        type.completeType
        !type.hasMultipleInstances()
        type.genericComponentType == new ParameterizedTypeImpl(String)
        type.instance == "asd"
        type.iterator().next() == type.instance
        type.allInstances == ["asd"] as Object[]
        type.improvableType == improvable
        type.toString().matches("String\\[] \\(\\w+\\)")

        when: "multiple instances"
        type = new GenericArrayInstanceType(String, "asd", "aad", "ddd")
        then:
        type.hasMultipleInstances()
        type.instance == "asd"
        type.allInstances == ["asd", "aad", "ddd"] as Object[]
        type.toString().matches("String\\[] \\(\\w+,...\\(3\\)\\)")

        when: "equals and hashcode cases"
        def type2 = new GenericArrayInstanceType(String, "asd")
        then:
        type.hashCode() == type2.hashCode()
        type.equals(type2)
        type.equals(new GenericArrayTypeImpl(String))
        type.equals(String[])
        !type.equals(WildcardTypeImpl.upper(List))
    }
}

package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*

/**
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
class TypesCompatibilityTest extends Specification {

    def "Check types compatibility"() {

        expect:
        TypeUtils.isCompatible(type1, type2) == res

        where:
        type1                          | type2                       | res
        String                         | Integer                     | false
        Object                         | Integer                     | true
        String                         | Object                      | true
        Base1                          | Root                        | true
        Root                           | Base1                       | true
        ArrayList                      | List                        | true
        List                           | ArrayList                   | true
        param(List, String)            | param(List, Integer)        | false
        param(List, Base1)             | param(List, Root)           | true
        param(List, Root)              | param(List, Base1)          | true
        array(String)                  | array(Integer)              | false
        array(Base1)                   | array(Root)                 | true
        array(Integer)                 | String                      | false
        String                         | array(Integer)              | false
        param(List, String)            | param(ArrayList, Integer)   | false
        param(ArrayList, String)       | param(List, Integer)        | false
        param(ArrayList, Base1)        | param(List, Root)           | true
        array(param(List, String))     | array(param(List, Integer)) | false
        array(param(ArrayList, Base1)) | array(param(List, Root))    | true
        new String[0].class            | new Integer[0].class        | false
        new Base1[0].class             | new Root[0].class           | true
        lower(String)                  | lower(String)               | true
        lower(String)                  | lower(Integer)              | false
        lower(Number)                  | lower(Integer)              | true
        lower(String)                  | String                      | true
        String                         | lower(String)               | true
        lower(Number)                  | Integer                     | false
        Integer                        | lower(Number)               | false
        lower(Integer)                 | upper(Number, Comparable)   | true
        upper(Number, Comparable)      | lower(Integer)              | true
        lower(Number)                  | Object                      | true
        lower(Object)                  | String                      | true
        String                         | lower(Object)               | true
        Object                         | lower(Number)               | true
        upper(Number, Comparable)      | Integer                     | true
        Integer                        | upper(Number, Comparable)   | true
        upper(Number, Comparable)      | String                      | false
        String                         | upper(Number, Comparable)   | false
        param(ArrayList, String)       | param(Iterable, Integer)    | false
    }

    def "Check types comparison"() {
        expect:
        TypeUtils.isMoreSpecific(type1, type2) == res

        where:
        type1                     | type2                     | res
        Base1                     | Root                      | false
        Root                      | Base1                     | true
        List                      | ArrayList                 | false
        ArrayList                 | List                      | true
        param(List, String)       | param(List, Object)       | true
        param(List, Base1)        | param(List, Root)         | false
        array(Base1)              | array(Root)               | false
        array(Root)               | array(Base1)              | true
        param(List, String)       | param(ArrayList, String)  | false
        param(ArrayList, String)  | param(List, String)       | true
        param(ArrayList, Object)  | param(ArrayList, String)  | false
        new Base1[0].class        | new Root[0].class         | false
        new Root[0].class         | new Base1[0].class        | true
        lower(String)             | lower(String)             | false
        lower(Number)             | lower(Integer)            | true
        lower(String)             | String                    | false
        String                    | lower(String)             | true
        lower(Integer)            | upper(Number, Comparable) | false
        upper(Number, Comparable) | lower(Integer)            | true
        lower(Number)             | Object                    | true
        lower(Object)             | String                    | false
        String                    | lower(Object)             | true
        Object                    | lower(Number)             | false
        upper(Number, Comparable) | Integer                   | false
        Integer                   | upper(Number, Comparable) | true
    }

    def "Check type comparison failure"() {

        when: "compare incompatible types"
        TypeUtils.isMoreSpecific(String, Integer)
        then: "err"
        def ex = thrown(IncompatibleTypesException)
        ex.message == "Type String can't be compared to Integer because they are not compatible"

        when: "incompatible generics on obviously specific types"
        TypeUtils.isMoreSpecific(param(ArrayList, String), param(Iterable, Integer))
        then: "err"
        ex = thrown(IncompatibleTypesException)
        ex.message == "Type ArrayList<String> can't be compared to Iterable<Integer> because they are not compatible"

        when: "incompatible generics on obviously less specific"
        TypeUtils.isMoreSpecific(param(Iterable, Integer), param(ArrayList, String))
        then: "err"
        ex = thrown(IncompatibleTypesException)
        ex.message == "Type Iterable<Integer> can't be compared to ArrayList<String> because they are not compatible"
    }

    def "Check specific type resolution"() {

        expect:
        TypeUtils.getMoreSpecificType(Base1, Root) == Root
    }
}

package ru.vyarus.java.generics.resolver


import ru.vyarus.java.generics.resolver.support.array.GenericArrayDeclarations
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*

/**
 * @author Vyacheslav Rusakov
 * @since 16.03.2019
 */
class CommonTypeResolutionTest extends Specification {

    def "Check common type calculation"() {

        expect:
        def type = TypeUtils.getCommonType(one, two)
        type == res
        TypeUtils.getCommonType(two, one) == res
        TypeToStringUtils.toStringType(type) // check for infinite to string

        where:
        one                                           | two                                            | res
        String                                            | String                                            | String
        Double                                            | Integer                                           | upper(Number, param(Comparable, Number))
        double                                           | int                                               | upper(Number, param(Comparable, Number))
        double                                           | Integer                                           | upper(Number, param(Comparable, Number))
        int                                               | int                                            | Integer
        param(Comparable, Number)                         | Comparable                                     | Comparable
        param(Comparable, Integer)                        | param(Comparable, Double)                      | param(Comparable, Number)
        Root1                                             | Base                                           | Base
        Root1                                             | Root2                                          | Base

        literal(new L<List<String>>(){})                  | literal(new L<List<String>>(){})               | literal(new L<List<String>>(){})
        literal(new L<List<Double>>(){})                  | literal(new L<List<Integer>>(){})              | literal(new L<List<Number>>(){})
        literal(new L<List<Double>>(){})                  | literal(new L<ArrayList<Integer>>(){})         | literal(new L<List<Number>>(){})
        literal(new L<HashMap<Integer, Set<Double>>>(){}) | literal(new L<Map<Double, List<Integer>>>(){}) | literal(new L<Map<Number, Collection<Number>>>(){})

        Double[]                                          | Integer[]                                      | array(upper(Number, param(Comparable, Number)))
        double[]                                          | int[]                                          | Object
        Double[]                                          | int[]                                          | Object
        Double[]                                          | Double                                         | Object
        GenericArrayDeclarations.doubleList             | GenericArrayDeclarations.integerList         | GenericArrayDeclarations.numberList
        GenericArrayDeclarations.stringList             | GenericArrayDeclarations.stringList          | GenericArrayDeclarations.stringList

        upper(Double)                                     | upper(Integer)                               | upper(Number, param(Comparable, Number))
        upper(Double, Cloneable)                          | upper(Integer, Cloneable)                    | upper(Number, param(Comparable, Number), Cloneable)
        upper(Comparable, Cloneable, Serializable)       | upper(Comparable, Serializable, ObjectInput)  | upper(Comparable, Serializable)
        upper(Integer, Cloneable, Root1)                | upper(Double, ObjectInput, Root1)              | upper(Number, param(Comparable, Number), Root1)
        upper(Integer, Cloneable, Root1)                | upper(Double, Comparable, Root2)               | upper(Number, param(Comparable, Number), Base)
        upper(Integer, Cloneable, Root1)                | upper(Number, Comparable, Root1)               | upper(Number, Comparable, Root1)
        upper(Integer, Cloneable, Root1)                | upper(Number, Comparable, Root2)               | upper(Number, Comparable, Base)
        upper(Integer, Cloneable)                       | String                                         | upper(param(Comparable, Serializable), Serializable)

        upper(Root1, Cloneable)                           | upper(Base, Comparable)                      | Base
        upper(Root1, Cloneable, Comparable)              | upper(Base, Comparable)                       | upper(Base, Comparable)
        upper(Root1, Cloneable, Comparable)              | upper(Root2, Comparable)                      | upper(Base, Comparable)
        upper(Root1, param(Comparable, String))          | upper(Root2, Comparable)                      | upper(Base, Comparable)
        upper(Root1, param(Comparable, Integer))         | upper(Root2, param(Comparable, Number))       | upper(Base, param(Comparable, Number))
        upper(Integer, Serializable)                   | upper(Cloneable, CharSequence)                  | Object
    }


    interface Base {}
    interface Root1 extends Base {}
    interface Root2 extends Base {}
}

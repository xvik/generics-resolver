package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.container.GenericArrayTypeImpl
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.context.container.WildcardTypeImpl
import ru.vyarus.java.generics.resolver.support.array.GenericArrayDeclarations
import ru.vyarus.java.generics.resolver.util.TypeUtils
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 16.03.2019
 */
class CommonTypeResolutionTest extends Specification {

    def "Check common type calculation"() {

        expect:
        TypeUtils.getCommonType(one, two) == res
        TypeUtils.getCommonType(two, one) == res

        where:
        one                                           | two                                            | res
        String                                            | String                                            | String
        Double                                            | Integer                                           | WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number))
        double                                           | int                                               | WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number))
        double                                           | Integer                                           | WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number))
        int                                               | int                                              | Integer
        new ParameterizedTypeImpl(Comparable, Number)     | Comparable                                        | Comparable
        new ParameterizedTypeImpl(Comparable, Integer)    | new ParameterizedTypeImpl(Comparable, Double)    | new ParameterizedTypeImpl(Comparable, Number)
        Root1                                              | Base                                             | Base
        Root1                                              | Root2                                            | Base

        new TypeLiteral<List<String>>(){}.getType()       | new TypeLiteral<List<String>>(){}.getType()       | new TypeLiteral<List<String>>(){}.getType()
        new TypeLiteral<List<Double>>(){}.getType()       | new TypeLiteral<List<Integer>>(){}.getType()      | new TypeLiteral<List<Number>>(){}.getType()
        new TypeLiteral<List<Double>>(){}.getType()       | new TypeLiteral<ArrayList<Integer>>(){}.getType() | new TypeLiteral<List<Number>>(){}.getType()
        new TypeLiteral<HashMap<Integer, Set<Double>>>(){}.getType() | new TypeLiteral<Map<Double, List<Integer>>>(){}.getType() | new TypeLiteral<Map<Number, Collection<Number>>>(){}.getType()

        Double[]                                          | Integer[]                                          | new GenericArrayTypeImpl(WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number)))
        double[]                                      | int[]                                          | Object
        Double[]                                      | int[]                                          | Object
        Double[]                                      | Double                                          | Object
        GenericArrayDeclarations.doubleList          | GenericArrayDeclarations.integerList     | GenericArrayDeclarations.numberList
        GenericArrayDeclarations.stringList          | GenericArrayDeclarations.stringList      | GenericArrayDeclarations.stringList

        WildcardTypeImpl.upper(Double)                | WildcardTypeImpl.upper(Integer)                | WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number))
        WildcardTypeImpl.upper(Double, Cloneable)     | WildcardTypeImpl.upper(Integer, Cloneable)     | WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number), Cloneable)
        WildcardTypeImpl.upper(Comparable, Cloneable, Serializable)     | WildcardTypeImpl.upper(Comparable, Serializable, ObjectInput)     | WildcardTypeImpl.upper(Comparable, Serializable)
        WildcardTypeImpl.upper(Integer, Cloneable, Root1)                | WildcardTypeImpl.upper(Double, ObjectInput, Root1)                 | WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number), Root1)
        WildcardTypeImpl.upper(Integer, Cloneable, Root1)                | WildcardTypeImpl.upper(Double, Comparable, Root2)                 | WildcardTypeImpl.upper(Number, new ParameterizedTypeImpl(Comparable, Number), Base)
        WildcardTypeImpl.upper(Integer, Cloneable, Root1)                | WildcardTypeImpl.upper(Number, Comparable, Root1)                  | WildcardTypeImpl.upper(Number, Comparable, Root1)
        WildcardTypeImpl.upper(Integer, Cloneable, Root1)                | WildcardTypeImpl.upper(Number, Comparable, Root2)                  | WildcardTypeImpl.upper(Number, Comparable, Base)
        WildcardTypeImpl.upper(Integer, Cloneable)                       | String                                                             | WildcardTypeImpl.upper(Serializable, new ParameterizedTypeImpl(Comparable, Serializable))

        WildcardTypeImpl.upper(Root1, Cloneable)                           | WildcardTypeImpl.upper(Base, Comparable)                 | Base
        WildcardTypeImpl.upper(Root1, Cloneable, Comparable)              | WildcardTypeImpl.upper(Base, Comparable)                  | WildcardTypeImpl.upper(Base, Comparable)
        WildcardTypeImpl.upper(Root1, Cloneable, Comparable)              | WildcardTypeImpl.upper(Root2, Comparable)                  | WildcardTypeImpl.upper(Base, Comparable)
        WildcardTypeImpl.upper(Root1, new ParameterizedTypeImpl(Comparable, String))              | WildcardTypeImpl.upper(Root2, Comparable)                  | WildcardTypeImpl.upper(Base, Comparable)
        WildcardTypeImpl.upper(Root1, new ParameterizedTypeImpl(Comparable, Integer))             | WildcardTypeImpl.upper(Root2, new ParameterizedTypeImpl(Comparable, Number))                  | WildcardTypeImpl.upper(Base, new ParameterizedTypeImpl(Comparable, Number))
    }


    interface Base {}
    interface Root1 extends Base {}
    interface Root2 extends Base {}
}

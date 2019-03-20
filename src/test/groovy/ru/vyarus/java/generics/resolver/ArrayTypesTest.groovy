package ru.vyarus.java.generics.resolver


import ru.vyarus.java.generics.resolver.support.array.GenericArrayDeclarations
import ru.vyarus.java.generics.resolver.util.ArrayTypeUtils
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 18.03.2019
 */
class ArrayTypesTest extends Specification {

    def "Check array operations"() {

        expect:
        ArrayTypeUtils.toArrayType(type) == arrayType
        ArrayTypeUtils.getArrayComponentType(arrayType) == type

        where:
        type                                         | arrayType
        byte                                         | byte[]
        char                                         | char[]
        double                                       | double[]
        float                                        | float[]
        int                                          | int[]
        long                                         | long[]
        short                                        | short[]
        boolean                                      | boolean[]
        List                                         | List[]
        // note: due to groovy bug(?) java type is required, because for groovy generic array is always simple array
        new TypeLiteral<List<String>>() {}.getType() | GenericArrayDeclarations.stringList
        int[]                                        | int[][]
        List[]                                        | List[][]

    }

    def "Check not array check"() {

        when: "get component type not on array"
        ArrayTypeUtils.getArrayComponentType(List)
        then:
        def ex = thrown(IllegalArgumentException)
        ex.message == "Provided type is not an array: List"

    }

    def "Check direct to array class"() {

        expect: "method generics dont cause problems"
        ArrayTypeUtils.toArrayClass(List) == List[]
        ArrayTypeUtils.toArrayClass(int) == int[]

    }
}

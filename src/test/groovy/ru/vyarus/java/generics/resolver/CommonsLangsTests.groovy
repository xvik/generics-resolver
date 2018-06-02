package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.CommonsLangChecks
import ru.vyarus.java.generics.resolver.util.GenericsTrackingUtils
import ru.vyarus.java.generics.resolver.util.GenericsUtils
import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Shared
import spock.lang.Specification

import java.lang.reflect.Method
import java.lang.reflect.Type

/**
 * Adopted tests of commons-lang3 TypeUtils.
 * https://github.com/apache/commons-lang/blob/d8ec011d770e1e04ef4f87fba673f3748f363278/src/test/java/org/apache/commons/lang3/reflect/TypeUtilsTest.java
 *
 * According to tests, generics-resolver is more permissive in isAssignable logic because generics assignment rules
 * are ignored - only actually known lower bounds are used.
 *
 * @author Vyacheslav Rusakov
 * @since 18.05.2018
 */
class CommonsLangsTests<B> extends Specification {

    @Shared
    static Type disType
    @Shared
    static Type datType
    @Shared
    static Type daType
    @Shared
    static Type uhderType
    @Shared
    static Type dingType
    @Shared
    static Type testerType
    @Shared
    static Type tester2Type
    @Shared
    static Type dat2Type

    void setupSpec() {
        disType = CommonsLangChecks.getField("dis").getGenericType();
        datType = CommonsLangChecks.getField("dat").getGenericType();
        daType = CommonsLangChecks.getField("da").getGenericType();
        uhderType = CommonsLangChecks.getField("uhder").getGenericType();
        dingType = CommonsLangChecks.getField("ding").getGenericType();
        testerType = CommonsLangChecks.getField("tester").getGenericType();
        tester2Type = CommonsLangChecks.getField("tester2").getGenericType();
        dat2Type = CommonsLangChecks.getField("dat2").getGenericType();
    }

    def "Check assignable"() {

        final Method method = CommonsLangChecks.getMethod("dummyMethod", List, List, List,
                List, List, List, List, List[].class, List[].class, List[].class, List[].class,
                List[].class, List[].class, List[].class);
        final Type[] types = method.getGenericParameterTypes();

        def type1 = types[pos1]
        def type2 = types[pos2]
        expect:
        println("$pos1 | $pos2 | ${prettyString(type1)} | ${prettyString(type2)} | ass: $assignable comp: $compatible spec: $moreSpecific")
        TypeUtils.isAssignable(type1, type2) == assignable
        TypeUtils.isCompatible(type1, type2) == compatible
        TypeUtils.isMoreSpecific(type1, type2) == moreSpecific

        where:
        pos1 | pos2 | assignable | compatible | moreSpecific
        0    | 0    | true       | true       | true       // List | List
        0    | 1    | true       | true       | true       // List | List<Object>
        1    | 0    | true       | true       | true       // List<Object> | List
        2    | 0    | true       | true       | true       // List<?> | List
        0    | 2    | true       | true       | true       // List | List<?>
        3    | 0    | true       | true       | true       // List<? super Object> | List
        0    | 3    | true       | true       | true       // List | List<? super Object>
        4    | 0    | true       | true       | true       // List<String> | List
        0    | 4    | true       | true       | false      // List | List<String>
        5    | 0    | true       | true       | true       // List<? extends String> | List
        0    | 5    | true       | true       | false      // List| List<? extends String>
        6    | 0    | true       | true       | true       // List<? super String> | List
        0    | 6    | true       | true       | false      // List | List<? super String>
        1    | 1    | true       | true       | true       // List<Object> | List<Object>
        2    | 1    | true       | true       | true       // List<?> | List<Object>
        1    | 2    | true       | true       | true       // List<Object> | List<?>
        3    | 1    | true       | true       | true       // List<? super Object> | List<Object>
        1    | 3    | true       | true       | true       // List<Object> | List<? super Object>
        4    | 1    | true       | true       | true       // List<String> | List<Object>
        1    | 4    | true       | true       | false      // List<Object> | List<String>
        5    | 1    | true       | true       | true       // List<? extends String> | List<Object>
        1    | 5    | true       | true       | false      // List<Object> | List<? extends String>
        6    | 1    | true       | true       | true       // List<? super String> | List<Object>
        1    | 6    | true       | true       | false      // List<Object> | List<? super String>
        2    | 2    | true       | true       | true       // List<?> | List<?>
        3    | 2    | true       | true       | true       // List<? super Object> | List<?>
        2    | 3    | true       | true       | true       // List<?> | List<? super Object>
        4    | 2    | true       | true       | true       // List<String> | List<?>
        2    | 4    | true       | true       | false      // List<?> | List<String>
        5    | 2    | true       | true       | true       // List<? extends String> | List<?>
        2    | 5    | true       | true       | false      // List<?> | List<? extends String>
        6    | 2    | true       | true       | true       // List<? super String> | List<?>
        2    | 6    | true       | true       | false      // List<?> | List<? super String>
        3    | 3    | true       | true       | true       // List<? super Object> | List<? super Object>
        4    | 3    | true       | true       | true       // List<String> | List<? super Object>
        3    | 4    | true       | true       | false      // List<? super Object> | List<String>
        5    | 3    | true       | true       | true       // List<? extends String> | List<? super Object>
        3    | 5    | true       | true       | false      // List<? super Object> | List<? extends String>
        3    | 6    | true       | true       | false      // List<? super Object> | List<? super String>
        4    | 4    | true       | true       | true       // List<String> | List<String>
        5    | 4    | true       | true       | true       // List<? extends String> | List<String>
        4    | 5    | true       | true       | true       // List<String> | List<? extends String>
        6    | 4    | false      | true       | false      // List<? super String> | List<String>
        4    | 6    | true       | true       | true       // List<String> | List<? super String>
        5    | 5    | true       | true       | true       // List<? extends String> | List<? extends String>
        6    | 5    | false      | true       | false     // List<? super String> | List<? extends String>
        5    | 6    | true       | true       | true      // List<? extends String> | List<? super String>
        6    | 6    | true       | true       | true      // List<? super String> | List<? super String>
        7    | 7    | true       | true       | true      // List[] | List[]
        8    | 7    | true       | true       | true      // List<Object>[] | List[]
        7    | 8    | true       | true       | true      // List[] | List<Object>[]
        9    | 7    | true       | true       | true      // List<?>[] | List[]
        7    | 9    | true       | true       | true      // List[] | List<?>[]
        10   | 7    | true       | true       | true      // List<? super Object>[] | List[]
        7    | 10   | true       | true       | true      // List[] | List<? super Object>[]
        11   | 7    | true       | true       | true      // List<String>[] | List[]
        7    | 11   | true       | true       | false     // List[] | List<String>[]
        12   | 7    | true       | true       | true      // List<? extends String>[] | List[]
        7    | 12   | true       | true       | false     // List[] | List<? extends String>[]
        13   | 7    | true       | true       | true      // List<? super String>[] | List[]
        7    | 13   | true       | true       | false     // List[] | List<? super String>[]
        8    | 8    | true       | true       | true      // List<Object>[] | List<Object>[]
        9    | 8    | true       | true       | true      // List<?>[] | List<Object>[]
        8    | 9    | true       | true       | true      // List<Object>[] | List<?>[]
        10   | 8    | true       | true       | true      // List<? super Object>[] | List<Object>[]
        8    | 10   | true       | true       | true      // List<Object>[] | List<? super Object>[]
        11   | 8    | true       | true       | true      // List<String>[] | List<Object>[]
        8    | 11   | true       | true       | false     // List<Object>[] | List<String>[]
        12   | 8    | true       | true       | true      // List<? extends String>[] | List<Object>[]
        8    | 12   | true       | true       | false     // List<Object>[] | List<? extends String>[]
        13   | 8    | true       | true       | true      // List<? super String>[] | List<Object>[]
        8    | 13   | true       | true       | false     // List<Object>[] | List<? super String>[]
        9    | 9    | true       | true       | true      // List<?>[] | List<?>[]
        10   | 9    | true       | true       | true      // List<? super Object>[] | List<?>[]
        9    | 10   | true       | true       | true      // List<?>[] | List<? super Object>[]
        11   | 9    | true       | true       | true      // List<String>[] | List<?>[]
        9    | 11   | true       | true       | false     // List<?>[] | List<String>[]
        12   | 9    | true       | true       | true      // List<? extends String>[]
        9    | 12   | true       | true       | false     // List<?>[] | List<? extends String>[]
        13   | 9    | true       | true       | true      // List<? super String>[] | List<?>[]
        9    | 13   | true       | true       | false     // List<?>[] | List<? super String>[]
        10   | 10   | true       | true       | true      // List<? super Object>[] | List<? super Object>[]
        11   | 10   | true       | true       | true      // List<String>[] | List<? super Object>[]
        10   | 11   | true       | true       | false     // List<? super Object>[] | List<String>[]
        12   | 10   | true       | true       | true      // List<? extends String>[] | List<? super Object>[]
        10   | 12   | true       | true       | false     // List<? super Object>[] | List<? extends String>[]
        13   | 10   | true       | true       | true      // List<? super String>[] | List<? super Object>[]
        10   | 13   | true       | true       | false     // List<? super Object>[] | List<? super String>[]
        11   | 11   | true       | true       | true      // List<String>[] | List<String>[]
        12   | 11   | true       | true       | true      // List<? extends String>[] | List<String>[]
        11   | 12   | true       | true       | true      // List<String>[] | List<? extends String>[]
        13   | 11   | false      | true       | false     // List<? super String>[] | List<String>[]
        11   | 13   | true       | true       | true      // List<String>[] | List<? super String>[]
        12   | 12   | true       | true       | true      // List<? extends String>[] | List<? extends String>[]
        13   | 12   | false      | true       | false     // List<? super String>[] | List<? extends String>[]
        12   | 13   | true       | true       | true      // List<? extends String>[] | List<? super String>[]
        13   | 13   | true       | true       | true      // List<? super String>[] | List<? super String>[]
    }


    def "Check assignable 2"() {

//        println GenericsResolver.resolve(Thing)

        expect:
        println("${prettyString(one)} | ${prettyString(two)} | ass: $assignable comp: $compatible spec: $moreSpecific")
        TypeUtils.isAssignable(one, two) == assignable
        TypeUtils.isCompatible(one, two) == compatible
        compatible ? TypeUtils.isMoreSpecific(one, two) == moreSpecific : true

        where:
        one         | two         | assignable | compatible | moreSpecific
        datType     | disType     | true       | true       | true      // That<String, String> | This<String, String>
        daType      | disType     | false      | false      | false     // The<String, String> | This<String, String>
        uhderType   | disType     | true       | true       | true      // Other<String> | This<String, String>
        dingType    | disType     | true       | true       | true     // Thing | This<String, String>
        disType     | dingType    | false      | true       | false      // This<String, String> | Thing
        testerType  | disType     | true       | true       | true      // Tester | This<String, String>
        tester2Type | disType     | true       | true       | true     // Tester | This<String, String>
        disType     | tester2Type | false      | true       | false      // This<String, String> | Tester
        dat2Type    | datType     | true       | true       | true      // That<String, String> | That<String, String>
        datType     | dat2Type    | true       | true       | true      // That<String, String> | That<String, String>
    }

    def "Check assignable 3"() {

        // https://docs.oracle.com/javase/specs/jls/se7/html/jls-5.html#jls-5.1.2
        expect: "widening rules are not supported (because not available for reflection)"
        println("${prettyString(one)} | ${prettyString(two)} | ass: $assignable comp: $compatible spec: $moreSpecific")
        TypeUtils.isAssignable(one, two) == assignable
        TypeUtils.isCompatible(one, two) == compatible
        compatible ? TypeUtils.isMoreSpecific(one, two) == moreSpecific : true



        where:
        one             | two            | assignable | compatible | moreSpecific
        char.class      | double.class   | false      | false      | false
        byte.class      | double.class   | false      | false      | false
        short.class     | double.class   | false      | false      | false
        int.class       | double.class   | false      | false      | false
        long.class      | double.class   | false      | false      | false
        float.class     | double.class   | false      | false      | false
        int.class       | long.class     | false      | false      | false
        Integer         | long.class     | false      | false      | false
        int.class       | Long           | false      | false      | false
        Integer         | Long           | false      | false      | false
        Integer.class   | int.class      | true       | true       | true
        int.class       | Integer        | true       | true       | true
        int.class       | Number         | true       | true       | true
        int.class       | Object         | true       | true       | true
        int.class       | Comparable     | true       | true       | true
        int.class       | Serializable   | true       | true       | true
        int[].class     | long[].class   | false      | false      | false
        Integer[].class | int[].class    | false      | false      | false
        int[].class     | Object[].class | false      | false      | false
        Integer[].class | Object[].class | true       | true       | true
    }

    def "Check assignable 4"() {

        when:
        final Type intComparableType = CommonsLangChecks.getField("intComparable").getGenericType();
        then:
        TypeUtils.isAssignable(int.class, intComparableType) // int | Comparable<Integer>

        when:
        final Type longComparableType = CommonsLangChecks.getField("longComparable").getGenericType();
        then:
        !TypeUtils.isAssignable(int.class, longComparableType)   // int | Comparable<Long>
        !TypeUtils.isAssignable(Integer.class, longComparableType) // Integer | Comparable<Long>

        when:
        final Type caType = CommonsLangChecks.getField("intWildcardComparable").getGenericType();
        then:
        TypeUtils.isAssignable(Integer[].class, caType)  // Integer[] | Comparable<? extends Integer>[]
        !TypeUtils.isAssignable(caType, Integer[].class) // Comparable<? extends Integer>[] | Integer[]  

        when:
        final Type bClassType = AClass.class.getField("bClass").getGenericType();
        final Type cClassType = AClass.class.getField("cClass").getGenericType();
        final Type dClassType = AClass.class.getField("dClass").getGenericType();
        final Type eClassType = AClass.class.getField("eClass").getGenericType();
        final Type fClassType = AClass.class.getField("fClass").getGenericType();
        then:
        TypeUtils.isAssignable(cClassType, bClassType)  // AClass$CClass<? extends String> | AClass$BClass<Number>
        TypeUtils.isAssignable(dClassType, bClassType)  // AClass$DClass<String> | AClass$BClass<Number>
        TypeUtils.isAssignable(eClassType, bClassType)  // AClass$EClass<String> | AClass$BClass<Number>
        TypeUtils.isAssignable(fClassType, bClassType)  // AClass$FClass | AClass$BClass<Number>
        TypeUtils.isAssignable(dClassType, cClassType)  // AClass$DClass<String> | AClass$CClass<? extends String>
        TypeUtils.isAssignable(eClassType, cClassType)  // AClass$EClass<String> | AClass$CClass<? extends String>
        TypeUtils.isAssignable(fClassType, cClassType)  // AClass$FClass |  AClass$CClass<? extends String>
        TypeUtils.isAssignable(eClassType, dClassType)  // AClass$EClass<String> | AClass$DClass<String>
        TypeUtils.isAssignable(fClassType, dClassType)  // AClass$FClass |  AClass$DClass<String>
        TypeUtils.isAssignable(fClassType, eClassType)  // AClass$FClass | AClass$EClass<java.lang.String>
    }

    def "Check generics resolution"() {

        expect:
        GenericsResolver.resolve(Integer)
                .type(Comparable)
                .generic("T") == Integer
        GenericsResolver.resolve(int)
                .type(Comparable)
                .generic("T") == Integer
        GenericsResolver.resolve(List)
                .type(Collection)
                .generic("E") == Object
        GenericsResolver.resolve(AAAClass.BBBClass)
                .type(AAClass.BBClass)
                .visibleGenericsMap() == ["T": Object, "S": String]
        GenericsResolver.resolve(CommonsLangChecks.Other)
                .type(CommonsLangChecks.This)
                .visibleGenericsMap() == ["B": Object, "K": String, "V": Object]
        GenericsResolver.resolve(CommonsLangChecks.And)
                .type(CommonsLangChecks.This)
                .visibleGenericsMap() == ["B": Object, "K": Number, "V": Number]
        GenericsResolver.resolve(CommonsLangChecks.Thing)
                .type(CommonsLangChecks.Other)
                .visibleGenericsMap() == ["B": Object, "T": Object]
    }

    def "Check bounded generics"() {

        expect:
        println("${prettyString(t1)} | ${prettyString(t2)}")
        TypeUtils.isCompatible(t1, t2)     // Comparable | Integer (for all cases)

        where:
        t1                                                                                      | t2
        GenericsResolver.resolve(getClass()).method(getClass().getMethod("stub")).generic("G")  | Integer
        GenericsResolver.resolve(getClass()).method(getClass().getMethod("stub2")).generic("G") | Integer
        GenericsResolver.resolve(getClass()).method(getClass().getMethod("stub3")).generic("T") | Integer
    }

    def "Check generics tracking"() {
        // Iterable<? extends Map<Integer, ? extends Collection<?>>>
        def iterableType = getClass().getField("iterable").getGenericType()

        expect: "tracking returns type, not just class"
        GenericsTrackingUtils.track(TreeSet, Iterable, ["T": iterableType])["E"] == iterableType
    }

    def "Check class resolution"() {

        GenericsContext context = GenericsResolver.resolve(getClass())
        final Type stringParentFieldType = getClass().getDeclaredField("stringParent")
                .getGenericType();
        final Type integerParentFieldType = getClass().getDeclaredField("integerParent")
                .getGenericType();
        final Type foosFieldType = getClass().getDeclaredField("foos").getGenericType();

        expect:
        context.resolveClass(stringParentFieldType) == GenericParent
        context.resolveGenericOf(stringParentFieldType) == String
        context.resolveClass(integerParentFieldType) == GenericParent
        context.resolveGenericOf(integerParentFieldType) == Integer
        context.resolveClass(foosFieldType) == List


        context.resolveGenericOf(foosFieldType) == Foo
        context.resolveClass(getClass()
                .getDeclaredField("barParents").getGenericType()) == GenericParent[].class
    }

    def "Check array resolution"() {

        Method method = CommonsLangChecks.getMethod("dummyMethod", List.class, List.class, List.class,
                List.class, List.class, List.class, List.class, List[].class, List[].class,
                List[].class, List[].class, List[].class, List[].class, List[].class);

        List<Type> types = GenericsResolver.resolve(CommonsLangChecks).method(method).resolveParametersTypes()

        expect:
        def clazz = GenericsUtils.resolveClass(types[pos], [:])
        println("${prettyString(types[pos])} | ${prettyString(clazz)} |  $res")
        clazz.isArray() == res

        where:
        pos | res
        0   | false
        1   | false
        2   | false
        3   | false
        4   | false
        5   | false
        6   | false
        7   | true
        8   | true
        9   | true
        10  | true
        11  | true
        12  | true
        13  | true
    }

    private String prettyString(Object type) {
        type.toString()
                .replace(CommonsLangChecks.getPackage().getName() + '.', '')
                .replace('java.lang.', '')
                .replace('java.util.', '')
                .replace('java.io.', '')
                .replace('[LList;', 'List[]')
                .replace('[LInteger;', 'Integer[]')
                .replace('[LObject;', 'Object[]')
                .replace('[I', 'int[]')
                .replace('[J', 'long[]')
                .replace('class', '')
                .replace('interface', '')
    }

    static class ClassWithSuperClassWithGenericType extends ArrayList<Object> {
        private static final long serialVersionUID = 1L;

        static <U> Iterable<U> methodWithGenericReturnType() {
            return null;
        }
    }

    public Iterable<? extends Map<Integer, ? extends Collection<?>>> iterable;

    static <G extends Comparable<G>> G stub() {
        return null;
    }

    static <G extends Comparable<? super G>> G stub2() {
        return null;
    }

    static <T extends Comparable<? extends T>> T stub3() {
        return null;
    }


    interface GenericConsumer<T> {
        void consume(T t);
    }

    static class GenericParent<T> implements GenericConsumer<T> {

        @Override
        void consume(final T t) {
        }

    }

    interface Foo {
        String VALUE = "foo";

        void doIt();
    }

    interface Bar {
        String VALUE = "bar";

        void doIt();
    }

    public GenericParent<String> stringParent;
    public GenericParent<Integer> integerParent;
    public List<Foo> foos;
    public GenericParent<Bar>[] barParents;
}

class AAClass<T> {

    public class BBClass<S> {
    }
}

class AAAClass extends AAClass<String> {
    public class BBBClass extends AAClass.BBClass<String> {
    }
}

@SuppressWarnings("rawtypes")
//raw types, where used, are used purposely
class AClass extends AAClass<String>.BBClass<Number> {

    AClass(final AAClass<String> enclosingInstance) {
    }

    public class BClass<T> {
    }

    public class CClass<T> extends BClass {
    }

    public class DClass<T> extends CClass<T> {
    }

    public class EClass<T> extends DClass {
    }

    public class FClass extends EClass<String> {
    }

    public class GClass<T extends BClass<? extends T> & AInterface<AInterface<? super T>>> {
    }

    public BClass<Number> bClass;

    public CClass<? extends String> cClass;

    public DClass<String> dClass;

    public EClass<String> eClass;

    public FClass fClass;

    public GClass gClass;

    interface AInterface<T> {
    }
}
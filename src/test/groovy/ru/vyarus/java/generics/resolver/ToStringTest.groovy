package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import ru.vyarus.java.generics.resolver.support.Model
import ru.vyarus.java.generics.resolver.support.array.ArBaseLvl2
import ru.vyarus.java.generics.resolver.support.inner.InOwner
import ru.vyarus.java.generics.resolver.support.tostring.Base
import ru.vyarus.java.generics.resolver.support.tostring.GenerifiedInterface
import ru.vyarus.java.generics.resolver.support.tostring.TSBase
import ru.vyarus.java.generics.resolver.support.tostring.TSRoot
import ru.vyarus.java.generics.resolver.support.wildcard.WCBase
import ru.vyarus.java.generics.resolver.support.wildcard.WCBaseLvl2
import ru.vyarus.java.generics.resolver.support.wildcard.WCRoot
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap
import spock.lang.Specification

import java.lang.reflect.Type

/**
 * @author Vyacheslav Rusakov 
 * @since 18.11.2014
 */
class ToStringTest extends Specification {
    def "Complex to string"() {

        when:
        GenericsContext context = GenericsResolver.resolve(TSRoot).type(TSBase)
        then:
        context.genericsAsString() == ["Model", "ArrayList<SType<Model, Model[]>>"]

        then:
        context.toStringType(TSBase.getMethod("doSomth").getGenericReturnType()) == "Model[]"
    }

    def "Check unknown generics detection"() {

        when: "to string type with unknown generics"
        TypeToStringUtils.toStringType(TSBase.getTypeParameters()[0])
        then:
        def ex = thrown(UnknownGenericException)
        ex.message == "Generic 'T' (defined on TSBase<T, K>) is not declared "
    }

    def "Complex to string 2"() {

        when: "resolving all types of interface generics"
        GenericsContext context = GenericsResolver.resolve(Base).type(GenerifiedInterface)
        then: "everything is ok"
        context.genericsAsString() == ['Integer', 'String[]', 'List<String>', 'List<Set<String>>']
    }

    def "Parametrized to string"() {

        expect:
        TypeToStringUtils.toStringType(new ParameterizedTypeImpl(Model, String)) == "Model<String>"
        TypeToStringUtils.toStringType(new ParameterizedTypeImpl(Model, [String] as Class[], new ParameterizedTypeImpl(List, Long))) == "List<Long>.Model<String>"
    }

    def "Wildcards to string"() {

        when: "to string wildcard"
        GenericsContext context = GenericsResolver.resolve(WCRoot).type(WCBase)
        then: "correct"
        context.genericAsString(0) == "Model"
        context.genericAsString(1) == "? super Model"

        context.type(WCBaseLvl2).genericAsString(0) == "Model"
    }

    def "Inner context to string"() {

        when: "reasolve type with outer generics"
        GenericsContext context = GenericsResolver.resolve(InnerTypesTest.Root)
        GenericsContext innerContext = context.fieldType(InnerTypesTest.Root.getDeclaredField('target'))

        then: "to string properly selects type generics only"
        innerContext.toStringCurrentClass() == "Inner"
        innerContext.toStringCurrentClassDeclaration() == "Inner"

        when: "parametrized context type"
        innerContext = context.fieldType(InnerTypesTest.Root.getDeclaredField('ptarget'))

        then: "to string properly selects type generics only"
        innerContext.toStringCurrentClass() == "PInner<Integer>"
        innerContext.toStringCurrentClassDeclaration() == "PInner<K>"
    }

    def "Check primitive arrays to string"() {

        expect:
        TypeToStringUtils.toStringType(int[]) == "int[]"
    }

    def "Check object generics removal"() {

        expect:
        TypeToStringUtils.toStringType(type) == res
        where:
        type                                                                                               | res
        new ParameterizedTypeImpl(List, Object)                                                            | "List"
        new ParameterizedTypeImpl(List, String)                                                            | "List<String>"
        new ParameterizedTypeImpl(ArBaseLvl2, Object, Object)                                              | "ArBaseLvl2"
        new ParameterizedTypeImpl(ArBaseLvl2, String, Object)                                              | "ArBaseLvl2<String, Object>"
        new ParameterizedTypeImpl(InOwner.Inner, [] as Type[], new ParameterizedTypeImpl(InOwner, Object)) | "InOwner.Inner"
        new ParameterizedTypeImpl(InOwner.Inner, [] as Type[], new ParameterizedTypeImpl(InOwner, String)) | "InOwner<String>.Inner"
    }

    def "Check to string with generics"() {

        expect:
        TypeToStringUtils.toStringWithGenerics(type, IgnoreGenericsMap.getInstance()) == res
        where:
        type                           | res
        List                           | "List"
        ArBaseLvl2                     | "ArBaseLvl2"
        InOwner.Inner                  | "Inner"
    }
}
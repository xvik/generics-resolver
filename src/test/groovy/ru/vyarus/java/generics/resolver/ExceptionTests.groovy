package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.error.GenericsResolutionException
import ru.vyarus.java.generics.resolver.error.GenericsTrackingException
import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException
import ru.vyarus.java.generics.resolver.error.UnknownGenericException
import ru.vyarus.java.generics.resolver.support.Base1
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.util.GenericsResolutionUtils
import ru.vyarus.java.generics.resolver.util.map.IgnoreGenericsMap
import spock.lang.Specification

import static ru.vyarus.java.generics.resolver.util.type.TypeFactory.*

import java.lang.reflect.ParameterizedType
import java.lang.reflect.TypeVariable

/**
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
class ExceptionTests extends Specification {

    def "Check unknown generic exception"() {

        when: "complete initialization"
        def res = new UnknownGenericException(Root, "T", null)
        then:
        res.genericName == "T"
        res.contextType == Root
        res.genericSource == null
        res.message == "Generic 'T' is not declared on type ${Root.name}"

        when: "rethrow with same typs"
        def reres = res.rethrowWithType(Root)
        then: "same instance"
        reres == res

        when: "rethrow with different type"
        res.rethrowWithType(Base1)
        then:
        def ex = thrown(IllegalStateException)
        ex.message == "Context type can't be changed"


        when: "incomplete initialization"
        res = new UnknownGenericException("T", null)
        then:
        res.genericName == "T"
        res.contextType == null
        res.genericSource == null
        res.message == "Generic 'T' is not declared "

        when: "change context"
        reres == res.rethrowWithType(Root)
        then: "different instance"
        reres != res
        reres.contextType == Root
        reres.genericName == "T"
        res.genericSource == null
        reres.message == "Generic 'T' is not declared on type ${Root.name}"
    }

    def "Check unknown generic exception with source"() {

        when: "unknown class generic"
        TypeVariable type = UnknownGeneric.getDeclaredField("field").getGenericType()
        def ex = new UnknownGenericException(type.name, type.genericDeclaration)
        then:
        ex.message == "Generic 'T' (defined on ExceptionTests.UnknownGeneric<T>) is not declared "
        ex.genericSource != null

        when: "unknown method generic"
        type = UnknownGeneric.getDeclaredMethod("method").getGenericReturnType()
        ex = new UnknownGenericException(type.name, type.genericDeclaration)
        then:
        ex.message == "Generic 'M' (defined on ExceptionTests.UnknownGeneric#<M> M method()) is not declared "
        ex.genericSource != null
    }

    def "Check resolution exception"() {

        when: "no knowns"
        def res = new GenericsResolutionException(Base1,
                GenericsResolutionUtils.resolveGenerics(Base1, new IgnoreGenericsMap()),
                Collections.emptyMap(), null)
        then:
        res.getType() == Base1
        res.getRootGenerics()["T"] == Object
        res.getKnownGenerics().isEmpty()
        res.message == "Failed to analyze hierarchy for Base1"

        when: "with known middles"
        res = new GenericsResolutionException(Base1,
                GenericsResolutionUtils.resolveGenerics(Base1, new IgnoreGenericsMap()),
                [(Lvl2Base1): ["I": String]], null)
        then:
        res.getType() == Base1
        res.getRootGenerics()["T"] == Object
        res.getKnownGenerics()[Lvl2Base1]["I"] == String
        res.message == "Failed to analyze hierarchy for Base1 (with known generics: Lvl2Base1<String>)"
    }

    def "Check tracking exception"() {

        when:
        def res = new GenericsTrackingException(Base1, Lvl2Base1, ["I": String], null)
        then:
        res.type == Base1
        res.knownType == Lvl2Base1
        res.knownTypeGenerics["I"] == String
        res.message == "Failed to track generics of Base1<T> from sub type Lvl2Base1<String>"
    }

    def "Check incompatible types"() {

        when: "default message"
        def res = new IncompatibleTypesException(param(Base1, String), param(Lvl2Base1, Integer))
        then:
        ((ParameterizedType) res.first).rawType == Base1
        ((ParameterizedType) res.second).rawType == Lvl2Base1
        res.message == "Incompatible types: Base1<String> and Lvl2Base1<Integer>"

        when: "custom message"

        res = new IncompatibleTypesException("custom %s %s", param(Base1, String), param(Lvl2Base1, Integer))
        then:
        ((ParameterizedType) res.first).rawType == Base1
        ((ParameterizedType) res.second).rawType == Lvl2Base1
        res.message == "custom Base1<String> Lvl2Base1<Integer>"
    }


    static class UnknownGeneric<T> {

        T field

        public <M> M method() {}
    }
}
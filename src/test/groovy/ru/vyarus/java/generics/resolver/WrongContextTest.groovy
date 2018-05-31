package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.error.WrongGenericsContextException
import ru.vyarus.java.generics.resolver.support.ConstructorGenerics
import spock.lang.Specification

import java.lang.reflect.TypeVariable

/**
 * @author Vyacheslav Rusakov
 * @since 30.05.2018
 */
class WrongContextTest extends Specification {

    def "Check wrong type context detection"() {

        when: "wrong context class"
        def context = GenericsResolver.resolve(Root)
        def res = context.resolveClass(Low.getDeclaredField("field").getGenericType())
        then: "context auto changed"
        res == String

        when: "wrong context for method generic"
        res = context.resolveClass(Low.getDeclaredMethod("get").getGenericReturnType())
        then: "context auto changed"
        res == Object

        when: "generic from class not in hierarchy"
        def type = Other.getDeclaredField("field").getGenericType()
        context.resolveClass(type)
        then:
        def ex = thrown(WrongGenericsContextException)
        ex.genericName == "E"
        ex.type == type
        ex.genericSource == ((TypeVariable) type).genericDeclaration
        ex.contextType == Root
        ex.message.replace('\r', '') == "Type E contains generic 'E' (defined on Other<E>) and can't be resolved in context of current class Root. Generic does not belong to any type in current context hierarchy:\n" +
                "class Root\n" +
                "  extends Low<String>\n"

        when: "target method differs from current context method"
        context.method(Low.getDeclaredMethod("get"))
        res = context.resolveClass(Low.getDeclaredMethod("get2").getGenericReturnType())
        then: "context auto changed"
        res == List

        when: "type from ignored class"
        context = GenericsResolver.resolve(Root, Low)
        type = Low.getDeclaredField("field").getGenericType()
        context.resolveClass(type)
        then:
        ex = thrown(WrongGenericsContextException)
        ex.genericName == "T"
        ex.type == type
        ex.genericSource == ((TypeVariable) type).genericDeclaration
        ex.contextType == Root
        ex.message.replace('\r', '') == "Type T contains generic 'T' (defined on Low<T>) and can't be resolved in context of current class Root. Generic declaration type Low is ignored in current context hierarchy:\n" +
                "class Root\n"

        when: "constructor generic"
        context = GenericsResolver.resolve(ConstructorGenerics)
        res = context.resolveClass(ConstructorGenerics.getConstructor(Comparable).getGenericParameterTypes()[0])
        then: "context auto changed"
        res == Comparable
    }

    def "Check wrong context error cases"() {

        // due to automatic context change some error type would never appear, but they are still supported

        when: "wrong context class"
        def context = GenericsResolver.resolve(Root)
        def type = Low.getDeclaredField("field").getGenericType()
        def ex = new WrongGenericsContextException(type, type, Root, context.genericsInfo)
        then:
        ex.genericName == "T"
        ex.message == "Type T contains generic 'T' (defined on Low<T>) and can't be resolved in context of current class Root. Switch context to handle generic properly: context.type(Low.class)"

        when: "wrong context for method generic"
        type = Low.getDeclaredMethod("get").getGenericReturnType()
        ex = new WrongGenericsContextException(type, type, Root, context.genericsInfo)
        then:
        ex.genericName == "K"
        ex.message == "Type K contains generic 'K' (defined on Low#<K> K get()) and can't be resolved in context of current class Root. Switch context to handle generic properly: context.method(Low.class.getDeclaredMethod(\"get\"))"

        when: "wrong context for different method"
        type = Low.getDeclaredMethod("get2").getGenericReturnType()
        ex = new WrongGenericsContextException(type, Low.getDeclaredMethod("get2").getTypeParameters()[0], Root, context.genericsInfo)
        then:
        ex.genericName == "M"
        ex.message == "Type List<M> contains generic 'M' (defined on Low#<M> List<M> get2()) and can't be resolved in context of current class Root. Switch context to handle generic properly: context.method(Low.class.getDeclaredMethod(\"get2\"))"

        when: "generic from class not in hierarchy"
        type = Other.getDeclaredField("field").getGenericType()
        ex = new WrongGenericsContextException(type, type, Root, context.genericsInfo)
        then:
        ex.genericName == "E"
        ex.message.replace('\r', '') == "Type E contains generic 'E' (defined on Other<E>) and can't be resolved in context of current class Root. Generic does not belong to any type in current context hierarchy:\n" +
                "class Root\n" +
                "  extends Low<String>\n"

        when: "type from ignored class"
        context = GenericsResolver.resolve(Root, Low)
        type = Low.getDeclaredField("field").getGenericType()
        ex = new WrongGenericsContextException(type, type, Root, context.genericsInfo)
        then:
        ex.genericName == "T"
        ex.message.replace('\r', '') == "Type T contains generic 'T' (defined on Low<T>) and can't be resolved in context of current class Root. Generic declaration type Low is ignored in current context hierarchy:\n" +
                "class Root\n"

        when: "constructor generic"
        context = GenericsResolver.resolve(ConstructorGenerics)
        type = ConstructorGenerics.getConstructor(Comparable).getGenericParameterTypes()[0]
        ex = new WrongGenericsContextException(type, type, ConstructorGenerics, context.genericsInfo)
        then:
        ex.genericName == "P"
        ex.message == "Type P contains generic 'P' (defined on <P> ConstructorGenerics(P)) and can't be resolved in context of current class ConstructorGenerics. Switch context to handle generic properly: context.constructor(ConstructorGenerics.class.getConstructor(Comparable.class))"

    }

    static class Low<T> {
        T field

        public <K> K get() {}

        public <M> List<M> get2() {}
    }

    static class Root extends Low<String> {}

    static class Other<E> {
        E field
    }
}

package ru.vyarus.java.generics.resolver.cases.doc

import ru.vyarus.java.generics.resolver.GenericsResolver
import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.error.WrongGenericsContextException
import spock.lang.Specification


/**
 * @author Vyacheslav Rusakov
 * @since 02.06.2018
 */
class AutoContextDocTest extends Specification {

    def "Check context detection"() {

        when: "resolving generics declared in hierarchy"
        GenericsContext context = GenericsResolver.resolve(Root.class)

        then:
        context.resolveClass(Base.getDeclaredField("field").getGenericType()) == Long
        context.resolveClass(Base2.getDeclaredField("field").getGenericType()) == String
        context.resolveClass(Base.getMethod("get").getGenericReturnType())== Comparable

        when: "resolving generic not in hierarchy"
        context.resolveClass(NotInside.class.getDeclaredField("field").getGenericType())
        then:
        true
        def ex = thrown(WrongGenericsContextException)
        ex.message.replace('\r', '') == """Type List<T> contains generic 'T' (defined on AutoContextDocTest.NotInside<T>) and can't be resolved in context of current class AutoContextDocTest.Root. Generic does not belong to any type in current context hierarchy:
class AutoContextDocTest.Root
  extends AutoContextDocTest.Base2<String>
    extends AutoContextDocTest.Base<Long>
"""
    }

    static class Base<T> {
        private T field;

        public <K extends Comparable> K get(){}
    }

    // generic with the same name
    static class Base2<T> extends Base<Long> {
        private T field;
    }

    static class Root extends Base2<String> {}

    // not in Root hierarchy
    static class NotInside<T> {
        private List<T> field;
    }
}
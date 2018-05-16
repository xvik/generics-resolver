package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.context.TypeGenericsContext
import ru.vyarus.java.generics.resolver.context.container.ParameterizedTypeImpl
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils
import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification

import java.lang.reflect.Type


/**
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
class InnerTypesTest extends Specification {

    def "Check inner class support"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Owner.Inner)
        def res = context.resolveFieldClass(Owner.Inner.getDeclaredField("field"))
        then:
        res == Object
    }

    def "Check parametrized inner support"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Owner.PInner)
        then:
        context.resolveFieldClass(Owner.PInner.getDeclaredField("field")) == Object
        context.resolveFieldClass(Owner.PInner.getDeclaredField("field2")) == Object

    }

    def "Check inlying context for inner type"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Root)
        TypeGenericsContext innerContext = context.fieldType(Root.getDeclaredField('target'))

        then:
        innerContext.resolveFieldClass(Owner.Inner.getDeclaredField('field')) == String
        innerContext.ownerTypeGenericsMap() == ['T': String]
    }

    def "Check inlying context for parametrized inner type"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Root)
        TypeGenericsContext innerContext = context.fieldType(Root.getDeclaredField('ptarget'))

        then:
        innerContext.resolveFieldClass(Owner.PInner.getDeclaredField('field')) == Integer
        innerContext.resolveFieldClass(Owner.PInner.getDeclaredField('field2')) == String
        innerContext.ownerTypeGenericsMap() == ['T': String]
    }

    def "Check inlying context for parametrized inner type with owner type in hierarchy"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Root)
        GenericsContext innerContext = context.fieldType(Root.getDeclaredField('htarget'))

        then:
        innerContext.resolveFieldClass(Owner.PInner.getDeclaredField('field')) == String
        innerContext.resolveFieldClass(Owner.PInner.getDeclaredField('field2')) == String
    }


    def "Check inner subtype"() {

        when:
        GenericsContext context = GenericsResolver.resolve(InnerExt)

        then:
        context.resolveFieldClass(InnerExt.getField('field')) == Object
    }

    def "Check inner subtype with parameters"() {

        when:
        GenericsContext context = GenericsResolver.resolve(PInnerExt)

        then:
        context.resolveFieldClass(PInnerExt.getField('field')) == Integer
        context.resolveFieldClass(PInnerExt.getField('field2')) == Object
    }

    def "Check owner generic used in hierarchy"() {

        when:
        GenericsContext context = GenericsResolver.resolve(Owner.HInner)

        then:
        context.resolveFieldClass(Owner.PInner.getField('field')) == Object
        context.resolveFieldClass(Owner.PInner.getField('field2')) == Object
    }

    def "Check inner type recognition from parameterized"() {

        setup: "intentionally not inner class to check that parametrized type used instead of class"
        Type type = new ParameterizedTypeImpl(Owner, [Integer] as Type[], Root)

        expect:
        TypeUtils.isInner(type) == true
        TypeUtils.getOuter(type) == Root
    }

    static class Owner<T> {

        class Inner {

            public T field
        }

        class PInner<K> {

            public K field
            public T field2
        }

        // use outer generic in hierarchy
        class HInner extends PInner<T> {

        }
    }

    static class Root extends Owner<String> {
        Inner target
        PInner<Integer> ptarget
        HInner htarget
    }

    static class InnerExt extends Owner.Inner {

    }

    static class PInnerExt extends Owner.PInner<Integer> {

    }
}
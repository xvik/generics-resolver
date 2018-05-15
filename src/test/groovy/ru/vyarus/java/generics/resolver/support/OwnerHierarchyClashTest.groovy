package ru.vyarus.java.generics.resolver.support

import ru.vyarus.java.generics.resolver.GenericsResolver
import spock.lang.Specification

import java.lang.reflect.Field

/**
 * @author Vyacheslav Rusakov
 * @since 14.05.2018
 */
class OwnerHierarchyClashTest extends Specification {

    def "Check owner hierarchy clash"() {

        // String  ---------------- Integer
        // new Root().inner = new Other().new Inner() // example when resolved types could contradict with runtime

        def inner = Owner.getDeclaredField("inner")
        def field = Owner.Inner.getDeclaredField("field")

        expect: "inner classes use generics to resolve inner type, but this leads to clas"
        // in any case consider this as correct behaviour - its more likely that inner class will be used inside
        // the same hierarchy
        GenericsResolver.resolve(Root).fieldType(inner).resolveFieldClass(field) == String
        GenericsResolver.resolve(Other).fieldType(inner).resolveFieldClass(field) == Integer
    }

    class Owner<T> {

        Inner inner

        class Inner {
            T field
        }
    }

    static class Root extends Owner<String> {
    }

    static class Other extends Owner<Integer> {
    }
}

package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.ComplexGenerics2
import ru.vyarus.java.generics.resolver.support.Lvl2Base1
import ru.vyarus.java.generics.resolver.support.Lvl2Base3
import ru.vyarus.java.generics.resolver.support.Root
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 15.05.2018
 */
class ContextToStringTest extends Specification {

    def "Type context to string"() {

        expect:
        toString(GenericsResolver.resolve(Root)) == """interface Root    <-- current
  extends Base1<Model>
    extends Lvl2Base1<Model>
  extends Base2<Model, OtherModel>
    extends Lvl2Base2<Model>
    extends Lvl2Base3<Model>
  extends ComplexGenerics<Model, List<Model>>
  extends ComplexGenerics2<Model[]>
"""

        toString(GenericsResolver.resolve(Root).type(Lvl2Base3)) == """interface Root
  extends Base1<Model>
    extends Lvl2Base1<Model>
  extends Base2<Model, OtherModel>
    extends Lvl2Base2<Model>
    extends Lvl2Base3<Model>    <-- current
  extends ComplexGenerics<Model, List<Model>>
  extends ComplexGenerics2<Model[]>
"""
    }

    def "Method context test"() {

        expect:
        toString(GenericsResolver.resolve(Root).method(ComplexGenerics2.getMethod("doSomth"))) == """interface Root
  extends Base1<Model>
    extends Lvl2Base1<Model>
  extends Base2<Model, OtherModel>
    extends Lvl2Base2<Model>
    extends Lvl2Base3<Model>
  extends ComplexGenerics<Model, List<Model>>
  extends ComplexGenerics2<Model[]>
    Model[] doSomth()    <-- current
"""


        toString(GenericsResolver.resolve(Root).method(Lvl2Base1.getMethod("doSomth4", Object, int))) == """interface Root
  extends Base1<Model>
    extends Lvl2Base1<Model>
      void doSomth4(Model, int)    <-- current
  extends Base2<Model, OtherModel>
    extends Lvl2Base2<Model>
    extends Lvl2Base3<Model>
  extends ComplexGenerics<Model, List<Model>>
  extends ComplexGenerics2<Model[]>
"""
    }

    def "Inlying context"() {

        def context = GenericsResolver.resolve(InnerTypesTest.Root).fieldType(InnerTypesTest.Root.getDeclaredField('htarget'))
        expect:
        toString(context) == """class InnerTypesTest.Owner<String>.HInner  resolved in context of InnerTypesTest.Root    <-- current
  extends InnerTypesTest.Owner<String>.PInner<String>
"""
        toString(context.type(InnerTypesTest.Owner.PInner)) == """class InnerTypesTest.Owner<String>.HInner  resolved in context of InnerTypesTest.Root
  extends InnerTypesTest.Owner<String>.PInner<String>    <-- current
"""

        toString(GenericsResolver.resolve(InnerTypesTest.Root).fieldType(InnerTypesTest.Root.getDeclaredField('ptarget'))) == """class InnerTypesTest.Owner<String>.PInner<Integer>  resolved in context of InnerTypesTest.Root    <-- current
"""
    }

    private toString(GenericsContext context) {
        return context.toString().replace("\r", "")
    }
}
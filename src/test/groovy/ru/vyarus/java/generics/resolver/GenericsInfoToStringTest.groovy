package ru.vyarus.java.generics.resolver

import ru.vyarus.java.generics.resolver.context.GenericsContext
import ru.vyarus.java.generics.resolver.support.BeanRoot
import ru.vyarus.java.generics.resolver.support.Root
import ru.vyarus.java.generics.resolver.support.array.ArRoot
import ru.vyarus.java.generics.resolver.support.brokenhieararchy.BrokenHierarchyRoot
import ru.vyarus.java.generics.resolver.support.noclash.NoClashRoot
import ru.vyarus.java.generics.resolver.support.wildcard.WCRoot
import spock.lang.Specification

/**
 * @author Vyacheslav Rusakov
 * @since 14.05.2018
 */
class GenericsInfoToStringTest extends Specification {

    def "Check context to string correctness"() {

        expect:
        toString(GenericsResolver.resolve(Root)) == """interface Root
  extends Base1<Model>
    extends Lvl2Base1<Model>
  extends Base2<Model, OtherModel>
    extends Lvl2Base2<Model>
    extends Lvl2Base3<Model>
  extends ComplexGenerics<Model, List<Model>>
  extends ComplexGenerics2<Model[]>
"""

        toString(GenericsResolver.resolve(BeanRoot)) == """class BeanRoot
  extends BeanBase<Model>
    extends Lvl2BeanBase<Model>
    implements Lvl2Base1<Model>
"""

        toString(GenericsResolver.resolve(BrokenHierarchyRoot)) == """class BrokenHierarchyRoot
  extends BrokenHierarchyBase<Callable, Object>
  implements BrokenHierarchyInterface<Callable, Object>
"""

        toString(GenericsResolver.resolve(ArRoot)) == """interface ArRoot
  extends ArBase<List<Model>, Model, ? super Model>
    extends ArBaseLvl2<List<Model>[], List<List<Model>>>
"""

        toString(GenericsResolver.resolve(NoClashRoot)) == """interface NoClashRoot
  extends NoClashSub1
    extends Runnable
    extends Callable<Integer>
  extends NoClashSub2
    extends Runnable
    extends Callable<Integer>
"""

        toString(GenericsResolver.resolve(WCRoot)) == """interface WCRoot
  extends WCBase<Model, ? super Model>
    extends WCBaseLvl2<Model>
"""
    }

    def "Check inlying contexts"() {

        expect:

        toString(GenericsResolver.resolve(InnerTypesTest.Owner.Inner)) == """class Owner.Inner
"""

        toString(GenericsResolver.resolve(InnerTypesTest.Owner.PInner)) == """class Owner.PInner
"""

        toString(GenericsResolver.resolve(InnerTypesTest.Root).fieldType(InnerTypesTest.Root.getDeclaredField('target'))) == """class Owner<String>.Inner
"""
    }

    private toString(GenericsContext context) {
        return context.genericsInfo.toString().replace("\r", "")
    }
}
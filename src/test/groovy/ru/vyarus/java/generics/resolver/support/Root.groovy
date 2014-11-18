package ru.vyarus.java.generics.resolver.support
/**
 * complex interface hierarchy to check generic types resolution
 *
 * @author Vyacheslav Rusakov 
 * @since 16.10.2014
 */
public interface Root extends Base1<Model>, Base2<Model, OtherModel>,
        ComplexGenerics<Model, List<Model>>, ComplexGenerics2<Model[]> {

}
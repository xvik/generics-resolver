package ru.vyarus.java.generics.resolver.support.nestedtype;

/**
 * @author Adam Biczok
 * @since 04.03.2015
 */
public class SubClass1<T>
        extends RootClass<NestedGenericType<T>>
        implements SubInterface<NestedGenericType<T>> {
}


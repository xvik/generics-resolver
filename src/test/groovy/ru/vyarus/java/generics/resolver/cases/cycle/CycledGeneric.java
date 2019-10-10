package ru.vyarus.java.generics.resolver.cases.cycle;

/**
 * @author Vyacheslav Rusakov
 * @since 10.05.2019
 */
public interface CycledGeneric<T extends CycledGeneric<T>> {
}

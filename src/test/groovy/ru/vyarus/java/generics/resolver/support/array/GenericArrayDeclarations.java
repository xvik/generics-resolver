package ru.vyarus.java.generics.resolver.support.array;

import ru.vyarus.java.generics.resolver.util.type.TypeLiteral;

import java.lang.reflect.Type;
import java.util.List;

/**
 * Have to use java because groovy ignore array generics and makes
 * {@code TypeLiteral<List[]>} instead of {@code TypeLiteral<List<String>[]>}
 *
 * @author Vyacheslav Rusakov
 * @since 18.03.2019
 */
public final class GenericArrayDeclarations extends TypeLiteral<List<String>[]> {

    public static Type stringList = new TypeLiteral<List<String>[]>(){}.getType();
    public static Type integerList = new TypeLiteral<List<Integer>[]>(){}.getType();
    public static Type doubleList = new TypeLiteral<List<Double>[]>(){}.getType();
    public static Type numberList = new TypeLiteral<List<Number>[]>(){}.getType();
}

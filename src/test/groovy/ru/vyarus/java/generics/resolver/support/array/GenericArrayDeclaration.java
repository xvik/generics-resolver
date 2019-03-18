package ru.vyarus.java.generics.resolver.support.array;

import ru.vyarus.java.generics.resolver.util.ArrayTypeUtils;
import ru.vyarus.java.generics.resolver.util.type.TypeLiteral;

import java.util.List;

/**
 * Have to use java because groovy ignore array generics and makes
 * {@code TypeLiteral<List[]>} instead of {@code TypeLiteral<List<String>[]>}
 *
 * @author Vyacheslav Rusakov
 * @since 18.03.2019
 */
public class GenericArrayDeclaration extends TypeLiteral<List<String>[]> {
    public static void main(String[] args) {
        Class<?> res = ArrayTypeUtils.toArrayClass(int.class);
    }
}

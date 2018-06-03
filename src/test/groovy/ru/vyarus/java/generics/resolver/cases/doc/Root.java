package ru.vyarus.java.generics.resolver.cases.doc;

/**
 * @author Vyacheslav Rusakov
 * @since 02.06.2018
 */
public class Root extends Outer<String, Integer, Long> {

    // field with inner class
    Inner<Boolean, String> field1;

    // field with inner class, but with direct outer generics definition
    Outer<String, String, String>.Inner<Boolean, String> field2;
}

package ru.vyarus.java.generics.resolver.support;

import java.util.List;

/**
 * @author Vyacheslav Rusakov
 * @since 20.05.2018
 */
public class CommonsLangChecks<B> {

    public interface This<K, V> {
    }

    public class That<K, V> implements This<K, V> {
    }

    public interface And<K, V> extends This<Number, Number> {
    }

    public class The<K, V> extends That<Number, Number> implements And<String, String> {
    }

    public class Other<T> implements This<String, T> {
    }

    public class Thing<Q> extends Other<B> {
    }

    public class Tester implements This<String, B> {
    }

    public This<String, String> dis;

    public That<String, String> dat;

    public The<String, String> da;

    public Other<String> uhder;

    public Thing ding;

    // impossible to declare like this in groovy
    public CommonsLangChecks<String>.Tester tester;

    public Tester tester2;

    public CommonsLangChecks<String>.That<String, String> dat2;

    public CommonsLangChecks<Number>.That<String, String> dat3;

    public Comparable<? extends Integer>[] intWildcardComparable;

    public static Comparable<Integer> intComparable;

    public static Comparable<Long> longComparable;

    public static Comparable<?> wildcardComparable;

    // required java source because groovy wipe away array generics
    public void dummyMethod(final List list0, final List<Object> list1, final List<?> list2,
                     final List<? super Object> list3,
                     final List<String> list4, final List<? extends String> list5,
                     final List<? super String> list6,
                     final List[] list7, final List<Object>[] list8, final List<?>[] list9,
                     final List<? super Object>[] list10,
                     final List<String>[] list11, final List<? extends String>[] list12,
                     final List<? super String>[] list13) {
    }
}

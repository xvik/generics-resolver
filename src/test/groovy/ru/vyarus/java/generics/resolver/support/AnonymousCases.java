package ru.vyarus.java.generics.resolver.support;

import java.util.HashMap;

/**
 * @author Vyacheslav Rusakov
 * @since 27.04.2019
 */
public class AnonymousCases {

    Object direct1 = new Object() {};
    Object direct2 = new Comparable() {
        @Override
        public int compareTo(Object o) {
            return 0;
        }
    };
    Object direct3 = new HashMap(){};

    Object ctor1;
    Object ctor2;
    Object ctor3;

    Object met1;
    Object met2;
    Object met3;

    public AnonymousCases() {
        ctor1 = new Object() {};
        ctor2 = new Comparable() {
            @Override
            public int compareTo(Object o) {
                return 0;
            }
        };
        ctor3 = new HashMap(){};

        method();
    }

    public void method() {
        met1 = new Object() {};
        met2 = new Comparable() {
            @Override
            public int compareTo(Object o) {
                return 0;
            }
        };
        met3 = new HashMap(){};
    }
}

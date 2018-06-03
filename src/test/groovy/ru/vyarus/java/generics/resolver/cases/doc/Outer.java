package ru.vyarus.java.generics.resolver.cases.doc;

/**
 * @author Vyacheslav Rusakov
 * @since 02.06.2018
 */
public class Outer<A, B, C> {

    public Outer() {
    }

    // constructor generic hides class generic C
    public <C> Outer(C arg) {
    }

    // method generic hides class generic A
    public <A> A doSmth() {
        return null;
    }

    // inner class hides outer generic (can't see)
    public class Inner<A, T> {

        // method generic hides outer class generic
        public <B> B doSmth2() {
            return null;
        }
    }
}

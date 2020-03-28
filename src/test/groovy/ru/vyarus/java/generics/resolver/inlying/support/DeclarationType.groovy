package ru.vyarus.java.generics.resolver.inlying.support

/**
 * @author Vyacheslav Rusakov
 * @since 07.05.2018
 */
// use type generics to check transitive resolution
class DeclarationType<Fld, Ret, Par> {

    private SubType<Fld> one
    private BaseIface<Fld> two
    private List<Map<Fld, Par>> someList

    public SubType<Ret> ret() {
        return null;
    }

    public void param(SubType<Par> par) {
    }
}

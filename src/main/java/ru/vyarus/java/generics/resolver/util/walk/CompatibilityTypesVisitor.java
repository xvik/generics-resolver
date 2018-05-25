package ru.vyarus.java.generics.resolver.util.walk;

import java.lang.reflect.Type;

/**
 * Checks if types hierarchies are compatible (one could be casted to another).
 * Completely rely on {@link TypesWalker} logic.
 *
 * @author Vyacheslav Rusakov
 * @since 11.05.2018
 */
public class CompatibilityTypesVisitor implements TypesVisitor {

    private boolean compatible = true;

    @Override
    public boolean next(final Type one, final Type two) {
        // all incompatible cases will be detected automatically
        return true;
    }

    @Override
    public void incompatibleHierarchy(final Type one, final Type two) {
        compatible = false;
    }

    /**
     * @return true when types are incompatible, false when compatible
     */
    public boolean isCompatible() {
        return compatible;
    }
}

package ru.vyarus.java.generics.resolver.util.walk;

/**
 * Check if one type is more specific. Stops when first type is not more specific.
 * Implicitly tracks compatibility: check {@link CompatibilityTypesVisitor#isCompatible()} before
 * calling {@link #isMoreSpecific()} as the later always assume compatible hierarchies.
 *
 * @author Vyacheslav Rusakov
 * @see TypesWalker
 * @since 11.05.2018
 */
public class ComparatorTypesVisitor extends CompatibilityTypesVisitor {

    private boolean moreSpecific = true;

    @Override
    public boolean next(final Class<?> one, final Class<?> two) {
        moreSpecific = two.isAssignableFrom(one);
        // if types are not compatible CompatibilityTypesVisitor will track that, so always assume compatibility
        // go further until more specific type detected in hierarchy
        return moreSpecific;
    }

    /**
     * @return true if first type is more specific, false otherwise
     */
    public boolean isMoreSpecific() {
        return moreSpecific;
    }
}

package ru.vyarus.java.generics.resolver.error;

/**
 * Thrown during type resolution when found generic name is not declared.
 * Could appear in two situations:
 * <ul>
 * <li>Type resolved in context of different class (usage error).</li>
 * <li>Type contains method generic. For example, in method {@code <T> void doSmth(List<T> arg1)} if
 * we try to resolve generic of arg1, it will fail, because generic T is only known within method scope
 * (resolve parameters and resolve method return type api correctly support such generics, but it's hard to support it
 * in general case).</li>
 * </ul>
 *
 * @author Vyacheslav Rusakov
 * @since 25.06.2015
 */
public class UnknownGenericException extends GenericsException {

    private final String genericName;
    private final Class contextType;

    /**
     * @param genericName generic name
     */
    public UnknownGenericException(final String genericName) {
        this(null, genericName);
    }

    /**
     * @param contextType context type (may be null)
     * @param genericName generic name
     */
    public UnknownGenericException(final Class contextType, final String genericName) {
        this(contextType, genericName, null);
    }

    public UnknownGenericException(final Class contextType, final String genericName, final Throwable cause) {
        super(String.format("Generic '%s' is not declared %s",
                genericName, contextType == null ? "" : "on type " + contextType.getName()), cause);
        this.contextType = contextType;
        this.genericName = genericName;
    }

    /**
     * @return generic name
     */
    public String getGenericName() {
        return genericName;
    }

    /**
     * @return context type where generic wasn't declared or null if type is unknown
     */
    public Class getContextType() {
        return contextType;
    }

    /**
     * Throw more specific exception.
     *
     * @param type context type
     * @return new exception if type is different, same exception instance if type is the same
     */
    public UnknownGenericException rethrowWithType(final Class type) {
        final boolean sameType = contextType != null && contextType.equals(type);
        if (!sameType && contextType != null) {
            // not allow changing type if it's already set
            throw new IllegalStateException("Context type can't be changed");
        }
        return sameType ? this : new UnknownGenericException(type, genericName, this);
    }
}

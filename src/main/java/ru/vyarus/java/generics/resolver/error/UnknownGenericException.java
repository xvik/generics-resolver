package ru.vyarus.java.generics.resolver.error;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

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
    private final Object genericSource;
    private final Class<?> contextType;

    /**
     * @param genericName generic name
     */
    public UnknownGenericException(final String genericName, final Object genericSource) {
        this(null, genericName, genericSource);
    }

    /**
     * @param contextType context type (may be null)
     * @param genericName generic name
     */
    public UnknownGenericException(final Class<?> contextType,
                                   final String genericName, final Object genericSource) {
        this(contextType, genericName, genericSource, null);
    }

    private UnknownGenericException(final Class<?> contextType,
                                    final String genericName, final Object genericSource,
                                    final Throwable cause) {
        super(String.format("Generic '%s'%s is not declared %s",
                genericName, formatSource(genericSource),
                contextType == null ? "" : "on type " + contextType.getName()), cause);
        this.contextType = contextType;
        this.genericName = genericName;
        this.genericSource = genericSource;
    }

    /**
     * @return generic name
     */
    public String getGenericName() {
        return genericName;
    }

    /**
     * @return generic declaration source if available or null
     */
    public Object getGenericSource() {
        return genericSource;
    }

    /**
     * @return context type where generic wasn't declared or null if type is unknown
     */
    public Class<?> getContextType() {
        return contextType;
    }

    /**
     * Throw more specific exception.
     *
     * @param type context type
     * @return new exception if type is different, same exception instance if type is the same
     */
    public UnknownGenericException rethrowWithType(final Class<?> type) {
        final boolean sameType = contextType != null && contextType.equals(type);
        if (!sameType && contextType != null) {
            // not allow changing type if it's already set
            throw new IllegalStateException("Context type can't be changed");
        }
        return sameType ? this : new UnknownGenericException(type, genericName, genericSource, this);
    }

    private static String formatSource(final Object source) {
        final String res;
        if (source != null) {
            final StringBuilder place = new StringBuilder();
            if (source instanceof Class) {
                place.append(TypeToStringUtils.toStringWithNamedGenerics((Class) source));
            } else if (source instanceof Method) {
                final Method method = (Method) source;
                place.append(method.getDeclaringClass().getSimpleName()).append('#');
                // append method generic declaration
                if (method.getTypeParameters().length > 0) {
                    place.append('<');
                    boolean first = true;
                    for (TypeVariable variable : method.getTypeParameters()) {
                        place.append(!first ? ", " : "").append(variable.getName());
                        first = false;
                    }
                    place.append("> ");
                }
                place.append(TypeToStringUtils.toStringMethod(method, new PrintableGenericsMap()));
            }
            // ignoring case of constructor generic (as not useful)

            res = place.length() == 0 ? "" : " (defined on " + place + ")";
        } else {
            // unknown source
            res = "";
        }
        return res;
    }
}

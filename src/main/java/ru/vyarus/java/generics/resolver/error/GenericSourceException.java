package ru.vyarus.java.generics.resolver.error;

import ru.vyarus.java.generics.resolver.context.GenericDeclarationScope;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.Constructor;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.TypeVariable;

/**
 * Base exception for unexpected generics errors.
 *
 * @author Vyacheslav Rusakov
 * @since 29.05.2018
 */
public abstract class GenericSourceException extends GenericsException {

    public GenericSourceException(final String message) {
        super(message);
    }

    public GenericSourceException(final String message, final Throwable cause) {
        super(message, cause);
    }

    /**
     * @return generic name
     */
    public abstract String getGenericName();

    /**
     * @return generic declaration source if available or null
     */
    public abstract GenericDeclaration getGenericSource();

    /**
     * @return context type where generic wasn't declared or null if type is unknown
     */
    public abstract Class<?> getContextType();


    /**
     * @param source generic declaration source
     * @return generic declaration source string or empty string when source is unknown
     */
    protected static String formatSource(final GenericDeclaration source) {
        final String res;
        final GenericDeclarationScope scope = GenericDeclarationScope.from(source);
        if (scope != null) {
            final StringBuilder place = new StringBuilder();
            switch (scope) {
                case METHOD:
                    final Method method = (Method) source;
                    place.append(method.getDeclaringClass().getSimpleName()).append('#')
                            // append method generic declaration
                            .append(formatGenerics(method.getTypeParameters()))
                            .append(TypeToStringUtils.toStringMethod(method, new PrintableGenericsMap()));
                    break;
                case CONSTRUCTOR:
                    final Constructor ctor = (Constructor) source;
                    // append constructor generic declaration
                    place.append(formatGenerics(ctor.getTypeParameters()))
                            .append(TypeToStringUtils.toStringConstructor(ctor, new PrintableGenericsMap()));
                    break;
                default:
                    place.append(TypeToStringUtils.toStringWithNamedGenerics((Class) source));
                    break;

            }

            res = place.length() == 0 ? "" : " (defined on " + place + ")";
        } else {
            // unknown source
            res = "";
        }
        return res;
    }

    private static String formatGenerics(final TypeVariable... generics) {
        final StringBuilder res = new StringBuilder(generics.length * 5);
        res.append('<');
        boolean first = true;
        for (TypeVariable variable : generics) {
            res.append(!first ? ", " : "").append(variable.getName());
            first = false;
        }
        res.append("> ");
        return res.toString();
    }
}

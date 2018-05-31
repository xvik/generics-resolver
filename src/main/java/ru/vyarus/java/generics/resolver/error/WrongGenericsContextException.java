package ru.vyarus.java.generics.resolver.error;

import ru.vyarus.java.generics.resolver.context.GenericDeclarationScope;
import ru.vyarus.java.generics.resolver.context.GenericsInfo;
import ru.vyarus.java.generics.resolver.util.GenericsUtils;
import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;
import ru.vyarus.java.generics.resolver.util.map.PrintableGenericsMap;

import java.lang.reflect.*;
import java.util.Arrays;

/**
 * Thrown to indicate generics not resolvable under current context class (because generics could use the same names
 * and incorrect context could lead to hard to track errors). Thrown only on methods inside
 * {@link ru.vyarus.java.generics.resolver.context.GenericsContext} where incoming type is explicitly checked
 * for compatibility (to prevent usage errors).
 * <p>
 * Most common reason is trying to resolve type without changing context type (for example, when using
 * {@code getMethod()} or {@code getField()}).
 * <p>
 * Error message contains usage hint if it is possible to resolve type correctly.
 *
 * @author Vyacheslav Rusakov
 * @since 28.05.2018
 */
public class WrongGenericsContextException extends GenericSourceException {

    private static final PrintableGenericsMap PRINTABLE_GENERICS = new PrintableGenericsMap();

    private final Type type;
    private final TypeVariable variable;
    private final Class<?> context;

    public WrongGenericsContextException(final Type type, final TypeVariable variable,
                                         final Class<?> context, final GenericsInfo info) {
        super(String.format(
                "Type %s contains generic '%s'%s and can't be resolved in context of current class %s. %s",
                TypeToStringUtils.toStringType(type, PRINTABLE_GENERICS),
                variable.getName(),
                formatSource(variable.getGenericDeclaration()),
                context.getSimpleName(),
                formatCompatibility(variable, info)));
        this.type = type;
        this.variable = variable;
        this.context = context;
    }

    /**
     * @return type, containing incompatible generic
     */
    public Type getType() {
        return type;
    }

    @Override
    public String getGenericName() {
        return variable.getName();
    }

    @Override
    public GenericDeclaration getGenericSource() {
        return variable.getGenericDeclaration();
    }

    @Override
    public Class<?> getContextType() {
        return context;
    }

    private static String formatCompatibility(final TypeVariable variable, final GenericsInfo info) {
        final Class<?> genericTarget = GenericsUtils.getDeclarationClass(variable);
        if (genericTarget == null) {
            // should be impossible
            return "";
        }
        final Class<?> requiredContext = info.findContextByDeclarationType(genericTarget);
        final String res;
        if (requiredContext != null) {
            final String nav;
            // context could be changed to handle type properly
            switch (GenericDeclarationScope.from(variable.getGenericDeclaration())) {
                case METHOD:
                    nav = String.format("context.method(%s)",
                            formatMethodGet((Method) variable.getGenericDeclaration()));
                    break;
                case CONSTRUCTOR:
                    nav = String.format("context.constructor(%s)",
                            formatConstructorGet((Constructor) variable.getGenericDeclaration()));
                    break;
                default:
                    nav = String.format("context.type(%s.class)", requiredContext.getSimpleName());
                    break;
            }
            res = "Switch context to handle generic properly: " + nav;
        } else {
            // type can't be resolved in current hierarchy
            if (Arrays.asList(info.getIgnoredTypes()).contains(genericTarget)) {
                // type ignored
                res = String.format("Generic declaration type %s is ignored in current context hierarchy:%n%s",
                        genericTarget.getSimpleName(), info.toString());
            } else {
                res = String.format("Generic does not belong to any type in current context hierarchy:%n%s",
                        info.toString());
            }
        }
        return res;
    }

    private static String formatMethodGet(final Method method) {
        final StringBuilder res = new StringBuilder(60);
        res.append(method.getDeclaringClass().getSimpleName())
                .append(".class.getDeclaredMethod(\"").append(method.getName()).append('"');
        formatParametersTail(res, false, method.getParameterTypes());
        return res.toString();
    }

    private static String formatConstructorGet(final Constructor ctor) {
        final StringBuilder res = new StringBuilder(60);
        res.append(ctor.getDeclaringClass().getSimpleName()).append(".class.getConstructor(");
        formatParametersTail(res, true, ctor.getParameterTypes());
        return res.toString();
    }

    private static void formatParametersTail(final StringBuilder res, final boolean first, final Class<?>... params) {
        boolean isFirst = first;
        for (Class par : params) {
            res.append(isFirst ? "" : ", ").append(par.getSimpleName()).append(".class");
            isFirst = false;
        }
        res.append(')');
    }
}

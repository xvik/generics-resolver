package ru.vyarus.java.generics.resolver.error;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exception indicates error during type's generics resolution process.
 *
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
@SuppressWarnings("PMD.LooseCoupling")
public class GenericsResolutionException extends GenericsException {

    private final Class<?> type;
    private final LinkedHashMap<String, Type> rootGenerics;
    private final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics;

    /**
     * @param type          root analyzed type
     * @param rootGenerics  root type's generics
     * @param knownGenerics possible knwon middle types generics
     * @param ex            actual cause
     */
    public GenericsResolutionException(final Class<?> type,
                                       final LinkedHashMap<String, Type> rootGenerics,
                                       final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics,
                                       final Exception ex) {
        super(String.format("Failed to analyze hierarchy for %s%s",
                TypeToStringUtils.toStringWithGenerics(type, rootGenerics),
                formatKnownGenerics(knownGenerics)), ex);
        this.type = type;
        this.rootGenerics = rootGenerics;
        this.knownGenerics = knownGenerics;
    }

    /**
     * @return resolution type (root analyzed hierarchy type)
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @return root type's generics or empty map if root type does not contains generics
     */
    public LinkedHashMap<String, Type> getRootGenerics() {
        return rootGenerics;
    }

    /**
     * @return known generics of middle types or empty map if no generics are known
     */
    public Map<Class<?>, LinkedHashMap<String, Type>> getKnownGenerics() {
        return knownGenerics;
    }

    private static String formatKnownGenerics(final Map<Class<?>, LinkedHashMap<String, Type>> knownGenerics) {
        if (knownGenerics.isEmpty()) {
            return "";
        }
        final StringBuilder known = new StringBuilder(50);
        known.append(" (with known generics: ");
        boolean first = true;
        for (Map.Entry<Class<?>, LinkedHashMap<String, Type>> entry : knownGenerics.entrySet()) {
            known.append(first ? "" : ", ")
                    .append(TypeToStringUtils
                            .toStringWithGenerics(entry.getKey(), entry.getValue()));
            first = false;
        }
        known.append(')');
        return known.toString();
    }
}

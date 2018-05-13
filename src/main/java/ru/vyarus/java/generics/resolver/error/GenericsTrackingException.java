package ru.vyarus.java.generics.resolver.error;

import ru.vyarus.java.generics.resolver.util.TypeToStringUtils;

import java.lang.reflect.Type;
import java.util.LinkedHashMap;

/**
 * Exception indicates generics tracking error (from some known middle type to root type).
 *
 * @author Vyacheslav Rusakov
 * @since 13.05.2018
 */
@SuppressWarnings("PMD.LooseCoupling")
public class GenericsTrackingException extends GenericsException {

    private final Class<?> type;
    private final Class<?> knownType;
    private final LinkedHashMap<String, Type> knownTypeGenerics;

    public GenericsTrackingException(final Class<?> type,
                                     final Class<?> knownType,
                                     final LinkedHashMap<String, Type> knownTypeGenerics,
                                     final Exception ex) {
        super(String.format("Failed to track generics of %s from sub type %s",
                TypeToStringUtils.toStringWithNamedGenerics(type),
                TypeToStringUtils.toStringWithGenerics(knownType, knownTypeGenerics)), ex);
        this.type = type;
        this.knownType = knownType;
        this.knownTypeGenerics = knownTypeGenerics;
    }

    /**
     * @return target type (hierarchy root) to track generics for
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @return type (in the middle of target type hierarchy) with known generics (tracking from).
     */
    public Class<?> getKnownType() {
        return knownType;
    }

    /**
     * @return known type's generics
     */
    public LinkedHashMap<String, Type> getKnownTypeGenerics() {
        return knownTypeGenerics;
    }
}

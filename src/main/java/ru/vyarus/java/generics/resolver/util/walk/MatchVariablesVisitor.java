package ru.vyarus.java.generics.resolver.util.walk;

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Match {@link ExplicitTypeVariable} in one type with real types from other type.
 *
 * @author Vyacheslav Rusakov
 * @since 14.12.2018
 */
public class MatchVariablesVisitor implements TypesVisitor {
    private final Map<TypeVariable, Type> matched = new HashMap<TypeVariable, Type>();
    private final Map<String, Type> matchedMap = new HashMap<String, Type>();
    private boolean hierarchyError;

    @Override
    public boolean next(final Type one, final Type two) {
        TypeVariable var = null;
        if (one instanceof ExplicitTypeVariable) {
            var = ((ExplicitTypeVariable) one).getDeclarationSource();
        } else if (one instanceof TypeVariable) {
            var = (TypeVariable) one;
        }
        if (var != null) {
            matched.put(var, two);
            matchedMap.put(var.getName(), two);
        }
        return true;
    }

    @Override
    public void incompatibleHierarchy(final Type one, final Type two) {
        hierarchyError = true;
    }

    public Map<TypeVariable, Type> getMatched() {
        return matched;
    }

    public Map<String, Type> getMatchedMap() {
        return matchedMap;
    }

    public boolean isHierarchyError() {
        return hierarchyError;
    }
}

package ru.vyarus.java.generics.resolver.util;

import ru.vyarus.java.generics.resolver.context.container.ExplicitTypeVariable;
import ru.vyarus.java.generics.resolver.error.IncompatibleTypesException;
import ru.vyarus.java.generics.resolver.error.UnknownGenericException;
import ru.vyarus.java.generics.resolver.util.walk.MatchVariablesVisitor;
import ru.vyarus.java.generics.resolver.util.walk.TypesWalker;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.*;

/**
 * Utility class for {@link TypeVariable} handling logic. Many api methods use type resolution
 * ({@link GenericsUtils#resolveTypeVariables(Type, Map)}) to get rid of type variables containing inside types.
 * Also, many apis will throw error about unknown type if they face {@link TypeVariable} inside.
 * <p>
 * To overcome this limitations in cases when you really need to preserve variable in type {@link ExplicitTypeVariable}
 * must be used. It is considered as normal type by all api methods.
 * <p>
 * There are two ways to replace types:
 * <ul>
 * <li>Use {@link #trackRootVariables(Class)} to preserve root class generics (useful for type
 * tracking)</li>
 * <li>Use {@link #preserveVariables(Type)} to replace all variables in type into {@link ExplicitTypeVariable}</li>
 * </ul>
 * <p>
 * Types with variables can be used as templates. To dynamically create real type from such template use
 * {@link #resolveAllTypeVariables(Type, Map)} (with required replacements provided in map). All type
 * variables could be found with {@link GenericsUtils#findVariables(Type)}.
 * <p>
 * Note that this class is considered as additional low level api. Use it only if you need to work with variables,
 * otherwise don't use (for example, don't use {@link TypeVariableUtils#resolveAllTypeVariables(Type, Map)} in all
 * cases, use usual {@link GenericsUtils#resolveTypeVariables(Type, Map)} instead.
 *
 * @author Vyacheslav Rusakov
 * @since 15.12.2018
 */
public final class TypeVariableUtils {

    private TypeVariableUtils() {
    }

    /**
     * Used when it is important to track when root type generics will go. For example:
     * {@code  class Root<T> implements Base<List<T>>}. When resolution applied, unknown root type T
     * will be replaced with special type {@link ExplicitTypeVariable}, which does not cause exceptions on type
     * resolution, but allows to preserve root type variables. After resolution actual types could be constructed
     * depending on root type parametrization (dynamic calculation).
     *
     * @param type          class to analyze
     * @param ignoreClasses classes to ignore during analysis (may be null)
     * @return resolved generics for all types in class hierarchy with root variables preserved
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static Map<Class<?>, LinkedHashMap<String, Type>> trackRootVariables(
            final Class<?> type,
            final List<Class<?>> ignoreClasses) {
        // leave type variables to track where would they go
        final LinkedHashMap<String, Type> rootGenerics = new LinkedHashMap<String, Type>();
        for (TypeVariable var : type.getTypeParameters()) {
            // special variables type, known by resolver (no exceptions for unknown generics will be thrown)
            rootGenerics.put(var.getName(), new ExplicitTypeVariable(var));
        }
        return GenericsResolutionUtils.resolve(type,
                rootGenerics,
                Collections.<Class<?>, LinkedHashMap<String, Type>>emptyMap(),
                ignoreClasses == null ? Collections.<Class<?>>emptyList() : ignoreClasses);
    }


    /**
     * Shortcut for {@link #trackRootVariables(Class, List)} to simplify usage without ignore classes.
     *
     * @param type class to analyze
     * @return resolved generics for all types in class hierarchy with root variables preserved
     */
    public static Map<Class<?>, LinkedHashMap<String, Type>> trackRootVariables(final Class type) {
        return trackRootVariables(type, null);
    }

    /**
     * Match explicit variables ({@link ExplicitTypeVariable}) in type with provided type. For example, suppose
     * you have type {@code List<E>} (with {@link ExplicitTypeVariable} as E variable) and
     * real type {@code List<String>}. This method will match variable E to String from real type.
     * <p>
     * WARNING: if provided template type will contain {@link TypeVariable} - they would not be detected!
     * Because method resolve all variables to its raw declaration. Use {@link #preserveVariables(Type)} in order
     * to replace variables before matching. It is not don't automatically to avoid redundant calls (this api
     * considered as low level api).
     *
     * @param template type with variables
     * @param real     type to compare and resolve variables from
     * @return map of resolved variables or empty map
     * @throws IncompatibleTypesException when provided types are not compatible
     * @see #trackRootVariables(Class, List) for variables tracking form root class
     * @see #preserveVariables(Type) for variables preserving in types
     */
    public static Map<TypeVariable, Type> matchVariables(final Type template, final Type real) {
        final MatchVariablesVisitor visitor = new MatchVariablesVisitor();
        TypesWalker.walk(template, real, visitor);
        if (visitor.isHierarchyError()) {
            throw new IncompatibleTypesException(
                    "Type %s variables can't be matched from type %s because they "
                            + "are not compatible", template, real);
        }
        final Map<TypeVariable, Type> res = visitor.getMatched();
        // to be sure that right type does not contain variables
        for (Map.Entry<TypeVariable, Type> entry : res.entrySet()) {
            entry.setValue(resolveAllTypeVariables(entry.getValue(), visitor.getMatchedMap()));
        }
        return res;
    }

    /**
     * Shortcut for {@link #matchVariables(Type, Type)} which return map of variable names instaed of raw
     * variable objects.
     *
     * @param template type with variables
     * @param real     type to compare and resolve variables from
     * @return map of resolved variables or empty map
     * @throws IllegalArgumentException when provided types are nto compatible
     */
    public static Map<String, Type> matchVariableNames(final Type template, final Type real) {
        final Map<TypeVariable, Type> match = matchVariables(template, real);
        if (match.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, Type> res = new HashMap<String, Type>();
        for (Map.Entry<TypeVariable, Type> entry : match.entrySet()) {
            res.put(entry.getKey().getName(), entry.getValue());
        }
        return res;
    }

    /**
     * The same as {@link GenericsUtils#resolveTypeVariables(Type[], Map)}, except it also process
     * {@link ExplicitTypeVariable} variables. Useful for special cases when variables tracking is used.
     * For example, type resolved with variables as a template and then used to create dynamic types
     * (according to context parametrization).
     *
     * @param types    types to resolve
     * @param generics root class generics mapping and {@link ExplicitTypeVariable} variable values.
     * @return types without variables
     */
    public static Type[] resolveAllTypeVariables(final Type[] types, final Map<String, Type> generics) {
        return GenericsUtils.resolveTypeVariables(types, generics, true);
    }

    /**
     * The same as {@link GenericsUtils#resolveTypeVariables(Type, Map)}, except it also process
     * {@link ExplicitTypeVariable} variables. Useful for special cases when variables tracking is used.
     * For example, type resolved with variables as a template and then used to create dynamic types
     * (according to context parametrization).
     *
     * @param type     type to resolve
     * @param generics root class generics mapping and {@link ExplicitTypeVariable} variable values.
     * @return resolved type
     * @throws UnknownGenericException when found generic not declared on type (e.g. method generic)
     * @see #preserveVariables(Type)
     */
    public static Type resolveAllTypeVariables(final Type type, final Map<String, Type> generics) {
        return GenericsUtils.resolveTypeVariables(type, generics, true);
    }

    /**
     * In contrast to {@link #resolveAllTypeVariables(Type, Map)} which replace generics in type according to
     * generics map, this method replace variables with their upper bound. For example, variable defined as
     * {@code class Root<T extends String>} and for type {@code List<T>} result will be {@code List<String>}
     * (variable T replaced by upper bound - String).
     *
     * @param type type to replace variables into.
     * @return type with all variables resolved as upper bound
     */
    public static Type resolveAllTypeVariables(final Type type) {
        final List<TypeVariable> vars = GenericsUtils.findVariables(type);
        if (vars.isEmpty()) {
            // no variables in type - nothing to replace
            return type;
        }

        final LinkedHashMap<String, Type> generics = new LinkedHashMap<String, Type>();
        // important to resolve vars in correct order
        for (TypeVariable var : GenericsUtils.orderVariablesForResolution(vars)) {
            generics.put(var.getName(), GenericsResolutionUtils.resolveRawGeneric(var, generics));
        }
        // finally resolve variables with pre-computed upper bounds
        return resolveAllTypeVariables(type, generics);
    }

    /**
     * Replace all {@link TypeVariable} into {@link ExplicitTypeVariable} to preserve variables.
     * This may be required because in many places type variables are resolved into raw declaration bound.
     * For example, useful for {@link TypesWalker} api.
     *
     * @param type type possibly containing variables
     * @return same type if it doesn't contain variables or type with all {@link TypeVariable} replaced by
     * {@link ExplicitTypeVariable}
     * @see #resolveAllTypeVariables(Type, Map) to replace explicit varaibles
     */
    @SuppressWarnings("PMD.AvoidInstantiatingObjectsInLoops")
    public static Type preserveVariables(final Type type) {
        final List<TypeVariable> vars = GenericsUtils.findVariables(type);
        if (vars.isEmpty()) {
            return type;
        }
        final Map<String, Type> preservation = new HashMap<String, Type>();
        for (TypeVariable var : vars) {
            preservation.put(var.getName(), new ExplicitTypeVariable(var));
        }
        // replace TypeVariable to ExplicitTypeVariable
        return GenericsUtils.resolveTypeVariables(type, preservation);
    }
}

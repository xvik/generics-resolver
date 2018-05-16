* Add type's full resolution method (in returned type all variables are replaced with actual known types): context.resolveType(Type).
    - add shortcut methods: context.resolveFieldType(Field), methodContext.resolveParameterType(pos), methodContext.resolveReturnType()   
* Support inlying contexts: class generics resolution in context of class (to correctly handle root class generics).
    Required for correct generics context building for field type, method return type or method parameter.      
    - context.inlyingType(Type) - universal resolver (the same as GenericsResolver.resolve(class) if class does not have generics (resolution cached))
    - context.fieldType(Field) - shortcut for fields (guarantee correct base type)
    - method(Method).returnType() - shortcut for method return type (guarantee correct base type)
    - method(Method).parameterType(pos) - shortcut for method parameter type (guarantee correct base type)
    - returned inlying context have reference to root context: TypeGenericsContext.rootContext()
* Type generics resolution with partial generics knowledge: building inlying context (using declared type information) for sub type.
    This is useful for instance analysis when you need to build generics context for actual object (with not known root generics), 
    but you know generics for declared middle type.
    - by analogy with direct inlying: inlyingTypeAs(Type, Class), fieldTypeAs(Field, Class), returnTypeAs(Class), parameterTypeAs(pos, Class)
    - tracks root type generics from known middle generics (e.g. Root<T> extends Base<T> when known Base<String> will resolve to Root<String>).
       Support composite generic declarations (and any hierarchy depth). 
* Low level analysis logic opened as utilities: GenericsResolutionUtils, GenericsTrackingUtils
* Types comparison api: TypesWalker.walk(Type, Type, Visitor) could walk two types side by side to check or compare actual classes on each level
    Usages:
    - GenericsResolutionUtils.isCompatible(Type, Type) - deep types compatibility check
    - GenericsResolutionUtils.isMoreSpecific(Type, Type) - types specificity check (e.g. to chose mroe specific like GenericsResolutionUtils.getMoreSpecificType(Type, Type))  
* Proper support for interface appearance in multiple hierarchy branches (appeared during analysis): 
    resolved generics from different branches now merged to use the most concrete known types
    (before it was failing if interface appears multiple times with different generics)
* (breaking) When type does not contain generics return empty list or null (NoGenericException checked exception removed) from
    context's .resolveGenericsOf() and .resolveGenericOf()
* (breaking) UnknownGenericsException moved to different package
* Add base type for all used exceptions: GenericsException (runtime exception) to simplify generic analysis errors interception
    - GenericsTrackingException - thrown on generics tracking problems
    - GenericsResolutionException - thrown on type hierarchy generics analysis problems
* TypeToStringUtils new methods:
    - toStringWithNamedGenerics() - print class with generic variables (List<E>)
    - toStringWithGenerics() - print class with known generics (List<Known>)
    - GenericsContext to string methods for context type: toStringCurrentClassDeclaration(), toStringCurrentClass()
* Add shortcuts for fields with automatic context switching (for less usage errors): context.resolveFieldClass, resolveFieldGenerics, resolveFieldGeneric
* Add class hierarchy to string print: GenericsInfo.toStringHierarchy() (context.getGenericsInfo())
    - All context classes now render default toString as complete class hierarchy with resolved generics and pointer to current location (for debug)
    - add methodContext().toStringMethod() to reneder method declartion with resolved generics
* Support outer class generics recognition during inner classes resolution: 
    - TypeGenericsContext.ownerTypeGenericsMap() returns used owner generics
    - for inlying context building, root class may be used as generics source for inner class (if root class hierachy contains outer class).
        This is not always true, but, for most cases, inner class is used inside outer and so generics resolution will be correct                                   

### 2.0.1 (2015-12-16)
* Fix dependent root generics resolution

### 2.0.0 (2015-06-27)
* Improve error reporting to show unknown generic name with analyzing type instead of NPE (simplifies usage errors understanding).
* Support method generics: new method context added (context.method(*)) to properly resolve generics including references to method generics
* (breaking change) method analysis methods (parameters and return type resolutions) are moved to method context. 
For example, before it was context.resolveParameters(method), now context.method(method).resolveParameters(). 

### 1.2.1 (2015-03-05)
* Improve duplicate interfaces support (thanks to [Adam Biczok](https://github.com/malary))

### 1.2.0 (2015-02-12)
* Root class generics now resolved (from generic bounds)
* Support broken hierarchies parsing (when root class generic passed or when target class did not set generics (as with root generics resolved from signature))

### 1.1.0 (2014-12-15)
* Add wildcards support
* Improve complex generics resolution
* Add access by generic name methods in GenericsContext
* Add ability to disable cache using property and method to clear current cache

### 1.0.0 (2014-11-19)
* Initial release
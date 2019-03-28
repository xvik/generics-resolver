* Add ArrayTypeUtils with operations on array types
* Add TypeVariableUtils for working with type templates (with preserved variables)
* Add TypeLiteral utility class to simplify complex types declaration (by analogy with guice's class)
* GenericUtils
    - Add orderVariablesForResolution method for ordering type variable declarations
    - findVariables now finds preserved variables (ExplicitTypeVariable) too
    - Fix findVariables to search for variables in variable declarations and avoid duplicate variables
    - Add shortcuts (to simplify common usages):
        - resolveClass(Type) (shortcut for EmptyGenericsMap.getInstance())
        - resolveClassIgnoringVariables(Type) (shortcut for IgnoreGenericsMap.getInstance())
* TypeToStringUtils
    - Add shortcuts (to simplify common usages):
        - toStringType(Type)
        - toStringTypeIgnoringVariables(Type)   
    - toStringType behaviour change: 
        - WildcardType prints types with generics nad not just classes as before
        - ParametrizedType avoid generics part when all are Object (e.g. now "List" instead of "List<Object>")
    - Add toStingType + join methods:
        - toStringTypes(Type[], Map<String, Type>) for comma separated types
        - toStringTypes(Type[], String, Map<String, Type>) for custom types separator            
* TypeUtils
    - Fix isAssignableBounds() for proper support of complex wildcards where none of left types is assignable 
       to all right types
    - Fix incompatible types detetion in isMoreSpecific() (check was stopped on types with obvious specificity)   
    - Add getCommonTypes(type1, type2): calculates base type assignable for both provided types               
* Improve types tracking: tracked types now analyzed for dependent variables to extract all possible type information
* Fix reversed generic variables declaration support (#3)
* Fix TypesWalker: processing should not continue after incompatible types detection
* TypeResolutionUtils
    - Add shortcut resolve(Class, LinkedHashMap<String, Type>, Class...) for resolution with known root generics
    - Add resolve(Type, Class...) to support resolution from ParameterizedType (and to be used as universal resolution method)      

### 3.0.0 (2018-06-19)
* Add constructor generics support
* Add inlying contexts support: generics context building for type "inside" of known hierarchy (field, method parameter etc): 
    "Drill down" case, when target type could contain generics known in current hierarchy (e.g. field type MyType<T>)     
    Context methods:          
    - context.inlyingType(Type) = GenericsContext - universal inlying context builder (the same as GenericsResolver.resolve(class) if class does not have generics)
    - context.fieldType(Field) - shortcut for fields (guarantee correct base type or error if type not in hierarchy)
    - method(Method).returnType() - shortcut for method return type (guarantee correct base type)
    - method(Method).parameterType(pos) - shortcut for method parameter type (guarantee correct base type)
    - constructor(Constructor).parameterType(pos) - shortcut fot constructor parameter
    - returned context have reference to root context: GenericsContext.rootContext()
* Inlying context building for higher type then declared (e.g. in field or method). Very special case, required for 
    instance analysis when not just type declarations, but also actual instance is used for analysis:
    Suppose we have field MyType<String\> inside class. But we know that actual instance is MySpecificType<T, K\> extends MyType<T\>.
    We need to build generics context for actual class (MySpecificType), but as we know base class type, we can track class generics
    as MySpecificType<String, Object\> (partially tracked) 
    - context.inlyingTypeAs(Type, Class) = GenericsContext - universal inlying context building for higher target class
    - Shortcuts, by analogy with simple inlying contexts: fieldTypeAs(Field, Class), returnTypeAs(Class), parameterTypeAs(pos, Class)
    - Target type generics tracking from known declared type generics (e.g. Root<T\> extends Base<T\> when known Base<String\> will resolve to Root<String\>).
       Support composite generic declarations (and any hierarchy depth). 
* Internal analysis logic opened as utilities (for low level usage without GenericsResolver) 
    - GenericsResolutionUtils - generics analysis logic (with access to analyzed type)
    - GenericsTrackingUtils - root generics tracking from middle class's known generics 
    - TypeUtils - generic utilities for types (ignoring unknown generics)
* Types deep comparison api: TypesWalker.walk(Type, Type, Visitor) could walk on two types side by side to check or compare actual classes on each level
    Usages:
    - TypeUtils.isCompatible(Type, Type) - deep types compatibility check
    - TypeUtils.isMoreSpecific(Type, Type) - types specificity check (e.g. to chose more specific like TypeUtils.getMoreSpecificType(Type, Type))
    - TypeUtils.isAssignable(Type, Type) - checks possibility to cast one type to another (according to known type information)  
* Improved support for multiple interface appearances in hierarchy: before exception was thrown if the same interface appears multiple times
    with different generics, now different declarations are merged to use the most specific types for interface. 
    Types compatibility explicitly checked during merge. 
* Reworked exceptions:
    - Now all exceptions extend base type GenericsException (runtime) to simplify generic analysis errors interception (catch(GenericException ex))
    - General tracking exception: GenericsTrackingException - thrown on generics tracking problems
    - General resolution exception: GenericsResolutionException - thrown on type hierarchy generics analysis problems
    - WrongGenericsContextException thrown when supplied type contains generics incompatible with current hierarchy 
        (not reachable from current context) 
    - More informative error messages
    - (breaking) UnknownGenericException moved to different package
    - (breaking) NoGenericException removed. Was thrown for resolveGenericsOf(Type) methods when class does not declare generics.
        Now empty list or null is returned.       
* Context api improvements:
    * Check all supplied types for compatibility with current class: throw exception when type contains generics
        belonging to other class (avoid usage errors)
    * Constructor generics support: context.constructor(ctor)
    * Kinds of visible generics:
        - genericsMap() - type own generics (as before)
        - ownerGenericsMap() - visible generics of outer class (if current is inner)
        - methodGenericsMap() - method generics
        - constructorGenericsMap() - constructor generics
        - visibleGenericsMap() - all visible generics (type +owner +method/constructor)
    * Type resolution methods (return type with all generic variables replaced with known values): 
        - resolveType(Type) = Type
        - Shortcuts: 
            - resolveFieldType(Field) - field type without generic variables 
            - resolveParameterType(pos) - (method context) parameter type without generic variables 
            - resolveReturnType() - (method context) return type without generic variables
        - resolveTypeGenerics(Type) = Type[]    
    * Shortcuts for common Field's resolutions:
        - resolveFieldClass(Field) - field class
        - resolveFieldGenerics(Field) - field's class generics (List<Class\>)
        - resolveFieldGeneric(Field) - field's class generic (Class) 
    * More toString utilities:
        - GenericsContext toString methods for context type: 
            - toStringCurrentClassDeclaration() - current with resolved generics ("MyClass<Integer>")
            - toStringCurrentClass() - current class with named generics ("MyClass<T>")
            - toStringMethod() - method string with resolved generics ("void do(MyType)")
            - toStringConstructor() - constructor string with resolved generics ("Some(Long)")
    * (breaking) resolveGenericsOf() called on Class will return upper bounds from generic declaration (previously returns empty map)                        
* Improved debugging support:
    - Core context could be printed as string (class hierarchy with resolved generics): context.getGenericsInfo().toString()
    - For customized context string rendering: context.getGenericsInfo().toStringHierarchy(TypeWriter) 
    - Direct toString() on context (GenericsContext) prints entire hierarchy with current position marker ("<-- current").
        In intellij idea, current context (with resolved generics) and position could be seen by using "view value" link inside debugger       
* Add inner classes support (Outer<T\>.Inner, not static) - outer class generics are resolved during hierarchy building: 
    - GenericsInfo contains maps with both type generics and owner generics
    - context.ownerClass() returns owner class, visible owner generics are accessible with context.ownerGenericsMap() (empty map for not inner class)
    - For inlying context building, root class may be used as generics source for inner class (if root class hierarchy contains owner class).
        This is not always true, but, for most cases, inner class is used inside owner and so generics resolution will be correct
* Improved bounds support:
    - Support multiple upper bounds declaration: My<T extends A & B\> now stored as wildcard (? extends A & B) and used
        for more precise checks (e.g. compatibility, assignability checks). As before, resolveClass("T") will use first upper bound (A).
    - (breaking) avoid upper bound wildcards (transform <? extends Something\> -> Something) as only type matter at runtime
        Affects hierarchy resolution: root generics will not contain Wildcards as before, but just type                                               

Compatibility notes:  
* API did not changed, only new methods were added.
    - removed GenericsUtils.getMethodParameters(method, generics): use instead resolveClasses(method.getGenericParameterTypes(), generics)
    - GenericsUtils.resolveGenericsOf() called on Class return upper bounds from generics definition (before was empty result) 
* NoGenericException was removed: detect generic absence by returned result instead
* UnknownGenericException: 
    - was moved to different package
    - now it is impossible to resolve generics in incorrect context, so UnknownGenericException is never thrown (when context api used), 
        instead WrongGenericsContextException could be thrown to indicate incompatible hierarchy
* It is not important anymore to set correct context (.type(..)): context now automatically switched to resolve generics in correct scope        
* Generics, previously resolved as <? extends Something\>, now become simply <Something\> (as upper wildcard not useful at runtime)  

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
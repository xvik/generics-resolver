* Inlying contexts: generics context building for type "inside" known hierarchy: 
    "Drill down" case, when new generics context must be build for some type, using generics of current context. 
    For example, we have some generics context and analyzing class fields. Some field is MyType<T> - generified with
    current class's generic. Inlying context is building new hierarchy with extra known generics (for example for class MyType<String>).
    Context methods:          
    - inlyingType(Type) = GenericsContext - universal inlying context builder (the same as GenericsResolver.resolve(class) if class does not have generics)
    - context.fieldType(Field) - shortcut for fields (guarantee correct base type or error if type not in hierarchy)
    - method(Method).returnType() - shortcut for method return type (guarantee correct base type)
    - method(Method).parameterType(pos) - shortcut for method parameter type (guarantee correct base type)
    - returned context have reference to root context: TypeGenericsContext.rootContext()
* Inlying context building for sub type: it's like inlying context (knowing type's root generics), but target type is 
    a subtype for current. Very special case, required for instance analysis: useful when not just type declarations, but
    also actual instance is used for analysis:
    Suppose we have field MyType<String> inside class. But we know that actual instance is MySpecificType<T, K> extends MyType<T>.
    We need to build generics context for actual class (MySpecificType), but as we know base class type, we can track class generics
    as MySpecificType<String, Object> (partially tracked) 
    - inlyingTypeAs(Type, Class) = GenericsContext - universal inlying context building for target class
    - Shortcuts, by analogy with simple inlying contexts: fieldTypeAs(Field, Class), returnTypeAs(Class), parameterTypeAs(pos, Class)
    - Tracks root type generics from known middle generics (e.g. Root<T> extends Base<T> when known Base<String> will resolve to Root<String>).
       Support composite generic declarations (and any hierarchy depth). 
* Internal analysis logic opened as utilities (for low level usage without GenericsResolver) 
    - GenericsResolutionUtils - generics analysis logic (with access to analyzed type)
    - GenericsTrackingUtils - root generics tracking from middle class's known generics (with access to analyzed types)
    - TypeUtils - generic utilities on types (ignoring unknown generics)
* Types deep comparison api: TypesWalker.walk(Type, Type, Visitor) could walk on two types side by side to check or compare actual classes on each level
    Usages:
    - TypeUtils.isCompatible(Type, Type) - deep types compatibility check
    - TypeUtils.isMoreSpecific(Type, Type) - types specificity check (e.g. to chose more specific like TypeUtils.getMoreSpecificType(Type, Type))  
* Improved support for multiple interface appearances in hierarchy: before exception was thrown if the same interface appears multiple times
    with different generics, now different declarations are merged to use the most specific types for interface. 
    Types compatibility explicitly checked during merge. 
* Reworked exceptions:
    - Now all exceptions extend base type GenericsException (runtime) to simplify generic analysis errors interception (catch(GenericException ex))
    - General tracking exception: GenericsTrackingException - thrown on generics tracking problems
    - General resolution exception: GenericsResolutionException - thrown on type hierarchy generics analysis problems
    - More informative error messages
    - (breaking) UnknownGenericException moved to different package
    - (breaking) NoGenericException removed. Was thrown for resolveGenericsOf(Type) methods when class does not declare generics.
        Now empty list or null will be returned.       
* Context api improvements:
    * Type resolution methods (return type with all generic variables replaced with known values): 
        - resolveType(Type) = Type
        - Shortcuts: 
            - resolveFieldType(Field) - field type without generic variables 
            - resolveParameterType(pos) - (method context) parameter type without generic variables 
            - resolveReturnType() - (method context) return type without generic variables
    * Shortcuts for Field's type resolution (with automatic context tracking to avoid silly mistakes):
        - resolveFieldClass(Field) - field class
        - resolveFieldGenerics(Field) - field's class generics (List<Class>)
        - resolveFieldGeneric(Field) - field's class generic (Class) 
    * More to string utilities:
        - GenericsContext to string methods for context type: 
            - toStringCurrentClassDeclaration() - current with resolved generics ("MyClass<Integer>")
            - toStringCurrentClass() - current class with named generics ("MyClass<T>")
        - toStringMethod() in method context - method string with resolved generics ("void do(MyType)")                
* Improved debugging support:
    - Core context could be printed as string (class hierarchy with resolved generics): context.getGenericsInfo().toString()
    - For customized context string rendering: context.getGenericsInfo().toStringHierarchy(TypeWriter) 
    - Direct toString() on context (GenericsContext) prints entire hierarchy with current position marker "<-- current" marker.
        In intellij idea you can use "view" value link inside debugger to quickly overview current context (with resolved generics) and position      
* Inner classes support (Outer<T>.Inner, not static): outer class generics are resolved to avoid UnknownGenericException 
    - Used owner class generics: TypeGenericsContext.ownerTypeGenericsMap() (empty map for not inner class)
    - For inlying context building, root class may be used as generics source for inner class (if root class hierachy contains outer class).
        This is not always true, but, for most cases, inner class is used inside outer and so generics resolution will be correct                                      

Compatibility notes: 
* API did not changed, only new methods were added. 
* NoGenericException was removed: detect generic absence by returned result instead
* UnknownGenericException was moved to different package

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
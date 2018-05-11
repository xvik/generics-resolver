* Add type's full resolution method (in returned type all variables are replaced with actual known types): context.resolveType(Type). 
* Support inlying contexts: class generics resolution in context of class (to correctly handle root class generics).
    Required for correct generics context building for field type, method return type or method parameter.      
    - context.inlyingType(Type) - universal resolver (the same as GenericsResolver.resolve(class) if class does not have generics (resolution cached))
    - context.inlyingType(Field) - shortcut for fields (guarantee correct base type)
    - method(Method).returnInlyingType() - shortcut for method return type (guarantee correct base type)
    - method(Method).parameterInlyingType(pos) - shortcut for method parameter type (guarantee correct base type)
    - returned inlying context have reference to root context: InlyingTypeGenericsContext.rootContext()
* Type generics resolution with partial generics knowledge: building inlying context (using declared type information) for sub type.
    This is useful for instance analysis when you need to build generics context for actual object (with not known root generics), 
    but you know generics for declared middle type.
    - by analogy with direct inlying: inlyingTypeAs(Type, Class), inlyingTypeAs(Field, Class), returnInlyingTypeAs(Class), parameterInlyingTypeAs(pos, Class)
    - tracks root type generics from known middle generics (e.g. Root<T> extends Base<T> when known Base<String> will resolve to Root<String>).
       Support composite generic declarations (and any hierarchy depth). 
* Low level analysis logic opened as utilities: GenericsResolutionUtils, GenericsTrackingUtils           

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
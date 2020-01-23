# Low level api

!!! note
    Context, produced by `GenericsResolver` is just a convenient utility
    to simplify usage. Internally it consists of a Map with resolved type generics and 
    utilities calls, which may be used directly.

    If known generics exists only as List, then it can be converted to map with:

    ```java
    Map<String, Type> generics = GenericsUtils.createGenericsMap(Some.class, knownGenericsList);
    ```

## Utilities

`TypeUtils` was already mentioned above - pure types operations (unknown generics ignored)
like `.isCompatible(Type, Type) == boolean`, `.getMoreSpecific(Type, Type)`, `.isAssignable(Type, Type)`

`TypeToStringUtils` - various to string helper methods  

`GenericsUtils` - generics manipulations (all `resolve*` methods from context) 
(requires known generics map to properly resolve types).

`GenericsResolutionUtils` - class analysis (mostly useful for root type resolution - hierarchy computation).
Creates generics maps, used for type resolutions. Special, and most useful case is direct class generics 
resolution (lower bounds): `GenericResolutionUtils.resolveRawGenetics(Class type) == Map<String, Type>`

`GenericsTrackingUtils` - resolution of root class's unknown generics by known middle class generics.
Used to compute more specific generics for root class before actual resolution (for inlying contexts).

`GenericInfoUtils` - `GenericsInfo` factory for all cases: direct class, sub type, and sub type with target class.
Essentially it's the same as GenericsResolver but without context wrapping (navigator) and without cache.

!!! warning 
    Some methods may not do what you expect! For example `TypeUtils.getOuter(Type)` is not the same as 
    `Classs#getEnclosingClass()` (which returns outer class for static classes and interfaces too).
    Another example is `ToStringUtils.toStringType()` which prints outer class only if provided type
    is ParameterizedType with not null owner. I essence, api oriented to generic resolution cases and
    all edge cases are described in javadoc.

## Special maps

Special maps may be used for generics resolution:

* `IgnoreGenericsMap` - use to ignore unknown generics (instead of fail).
For example, `GenericsUtils.resolveClass(List<T>, new IgnoreGenericsMap()) == List.class`
* `PrintableGenericsMap` - special map for `TypeToStringUtils` to print unkown generics (instead of fail).
For example, `TypeToStringUtils.toStringType(List<T>, new PrintableGenericsMap()) == "List<T>"`  

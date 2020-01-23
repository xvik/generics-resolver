# Base type generics 

Resolve generics from implemented interface or extended class.

Common case for various extension mechanisms is to know actual generics of 
some extension interface for actual extension class:
 
```java
interface Extension<V> {}

class ListExtension implements Extension<List<String>> {}
```

```java
GenericsResolver.resolve(ListExtension.class)
        .type(Extension.class)
        .genericType("V") == List<String> // (ParameterizedType) 
```
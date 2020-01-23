# Types sanitizer

You may already have some api for working with `Type` objects.
In this case resolver could be used to replace all generic variables with actual types:

```java
class Base<T> {
    List<T> field;    
}

class Root extends Base<Integer> {}
```

```java
GenericsContext context = GenericsResolver.resovle(Root.class);

// pure reflection, actual type is List<T>
Type fieldType = Base.class.getField("field");
// sanitize type to List<Integer>
fieldType = context.type(Base.class).resovleType(fieldType);

// continue working with pure type
```   

!!! note
    Type resolution is actually complete type repackage (if required) to replace
    `TypeVariable` in it with actual generic value.

Alternatively, if class hierarchy is not known and we want to remove
generics in context of current class only

```java
Type fieldType = GenericsUtils
            .resolveTypeVariables(fieldType, new IgnoreGenericsMap())
```

Will resolve type as `List<Object>` (unknown generic "T" resolved as Object.class). 

Sometimes it is useful to extract generics of type:

```java
// type's generics
Type[] typeGenerics = GenericsUtils.getGenerics(type, generics);
// type's with replaced variables 
Type[] sanitizedGenerics = GenericsUtils
            .resolveTypeVariables(typeGenerics, generics); 
``` 

Note: Generics map (generics) of host type could be taken form host type's generics context (`context.visibleGenericsMap()`) 
or manually (see generics map [direct resolution](direct.md) example).

For example, if original type was `Map<String, List<T>>` then sanitizedGenerics will be
`[String, List<Integer>]` (suppose T is Integer in current context).
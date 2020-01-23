# Utilities usage

In most cases, to use utility directly, all you need to know is type's generics map.
It could be either obtained from context (`context.type(Some.class).visibleGenericsMap()`)
or [resolved directly](direct.md). 

```java
class Some<T> {
    T field;
}
```

Suppose you have generics resolved as `generics` variable.

```java
Map<String, Type> generics = ["T": List<String>];
```

Note that this map must include all visible generics (including outer class, method or constructor if they 
could appear in target types), so better use context api to obtain complete generics map (otherwise UnkownGenericsException could arise at some point).

```java
Type field = Some.class.getDeclaredField("field");
GenericsUtils.resolveClass(field, generics) == List.class;
GenericsUtils.resolveGenericsOf(field, generics) == String.class;

TypeToStringUtils.toStringType(field, generics) == "List<String>";
TypeToStringUtils.toStringWithNamedGenerics(field) == "List<T>";
TypeToStringUtils.toStringWithGenerics(Some.class, generics) == "Some<List<String>>";
```

See context api implementation for more utilities cases (context use utilities for everything).

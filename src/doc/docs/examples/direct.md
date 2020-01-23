# Direct generics resolution

Generics resolution process is actually building a map of all types and their generics
(this map is used inside resolution context for types navigation).

```java
class Base<T extends Number> {}

class Root extends Base<Integer> {}
```

Get generics hierarchy:

```java
GenericsResolutionUtils.resolve(Root.class) == [Root.class : [], Base.class: ["T": Integer.class]];
``` 

Or just resolve generics resolution from definition:

```java
GenericsResolutionUtils.resolveRawGenerics(Base.class) == ["T": Number.class ];
```

Or, if generics already known as List, it could be converted to map:

```java
Map<String, Type> generics = GenericsUtils.createGenericsMap(Some.class, knownGenericsList);
``` 

For example, we know that `class MyType<T, K>` has `[String.class, Integer.class]` generics.
Then we can convert it to map: 

```java
GenericsUtils.createGenericsMap(MyType.class, [String.class, Integer.class]) 
                == ["T": String.class, "K": Integer.class]
```

And use resulted map for utility calls (e.g. `GenericsUtils` or `ToStringUtils`).
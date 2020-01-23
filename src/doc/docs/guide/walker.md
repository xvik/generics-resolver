# Types walker

Special api for walking on two types side by side. 
Usages: compatibility check and more specific type detection.

```java
TypesWalker.walk(Type, Type, TypesVisitor);
```

`TypesVisitor` implementation receive either incompatibility signal 
(and processing stops after that) or types for comparison. Visitor could stop processing at any stage.

For example, for `List<String>` and `List<Integer>`:

```
next(List, List)
incompatibleHierarchy(String, Integer)
```

It will correctly balance types by building hierarchy, where 
appropriate:

`List<MyCallable<String>>` and `ArrayList<Callable<String>`:

```
next(List, ArrayList)
// resolve generic for List on the right
next(MyCallable, Callable)
// now compute callable generic on the left
next(String, String)
```

## Types rules

*Java wildcard rules are not strictly followed* during type compatibility checks, because
many rules are useless at runtime.  

Object always assumed as not known type.

`List<?>` == `List` == `List<Object>` == `List<? super Object>` == `List<? extends Object>`

Object is compatible and assignable to everything. `Object` is assignable to `List<String>` and 
`List<String>` is assignable to `Object` (type not known - assuming compatibility). 

`<? extends Something>` is considered as just `Something`.

`<? super Something>` is compatible with any super type of `Something` but not with any
sub type (`SomethingExt extends Something`).

`Object` is assignable to `<? extends String>`, but later is more specific (contains more type information).

`<? super Number>` is assignable to `<? super Integer>`, but not opposite! 

Primitives are compared as wrapper types (e.g. `Integer` for `int`), but not primitive arrays!

Table below compares  different `TypeUtils` methods (implemented using walker api): 

| type 1 | type 2 | isAssignable | isCompatible | isMoreSpecific |
| ------ | ------ | ------------ | ------------ | -------------- |
| Object | List | + | + | - |
| String | Integer | - | - | - |
| List | Object | + | + | + |
| List | List | + | + | + |
| List | List&lt;String> | + | + | - |
| List&lt;String> | List&lt;Integer> | - | - | - |
| List&lt;String> | List | + | + | + |
| ArrayList | List&lt;String> | + | + | + |
| List&lt;String> | ArrayList | - | + | - |
| List&lt;String> | ArrayList&lt;String> | - | + | - |
| List | List&lt;? super String> | + | + | - |
| List&lt;? super String> | List | + | + | + |
| List&lt;? super Number> | List&lt;? super Integer> | + | + | + |
| List&lt;String> | List&lt;? super String> | + | + | + |
| List&lt;? super String> | List&lt;String> | - | + | - |
| List[] | List&lt;? super String>[] | + | + | - |
| List&lt;? super String>[] | List[] | + | + | + |
| Integer[] | Object[] | + | + | + |
| Object[] | Integer[] | + | + | - |
| Some&lt;String, Object> | Some&lt;String, String> | + | + | - |
| Some&lt;String, String> | Some&lt;String, Object> | + | + | + |
| Some&lt;String, Long> | Some&lt;String, Boolean> | - | - | - |
| Integer | long | - | - | - |
| Integer | int | + | + | + |
| int | Number | + | + | + |
| Number | int | - | + | - |
| int | Comparable | + | + | + |
| int | Comparable&lt;Long> | - | - | - |
| int | long | - | - | - | 
| int[] | long[] | - | - | - |
| int[] | Object[] | - | - | - |
| int[] | Integer[] | - | - | - |
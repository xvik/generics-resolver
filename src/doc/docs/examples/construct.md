# Construct custom types

Custom type containers (used internally for types repackaging) may be used for 
constructing types:

```java
new ParameterizedTypeImpl(List.class. String.class) // List<String>
WildcardTypeImpl.upper(String.class) // ? extends String
WildcardTYpeImpl.lower(String.class) // ? super String
new GenericArrayTypeImpl(String.class) // String[]
```

Note that upper bounded wildcard constructor allows multiple upper bounds, for example: 

```java
WildcardTypeImpl.upper(Number.class, Comparable.class)
``` 
which creates impossible (to declare in java) wildcard `? extends Number & Comparable`. This is used internally for 
multiple bounded variable declaration repackage (which could contain multiple bounds) : `T extends Number & Comparable`.
But, for example, groovy allows multiple bounds in wildcards.

Custom type container could be used in toString logic:

```java
TypeToStringUtils.toStringType(
        new ParameterizedTypeImpl(List.class. String.class),
        Collections.emptyMap()) == "List<String>";
```

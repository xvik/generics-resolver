# Types compatibility check

Suppose you have some class:

```java
class Conf {
    SubConf<List<String>> sub; 
}
```

And some other class:

```java
class Listener {
    listen(SubConf<List<Integer>> obj);
}
```

And you want to check if field value from `Conf` could be used in listener:

```java
// for more complex cases there may be generics resolution too 
// pure reflection just for clarity
Type listenerArg = Listener.class
                        .getMethod("listen", SubConf.class)
                        .getGenericParameterTypes()[0];
Type confField = Conf.getField("sub").getGenericType();

// not assignable because of list generics: String vs Integer
TypeUtils.isAssignable(confField, listnerArg) == false    
``` 

!!! note 
    Possible name variables (not resolved) in types are replaced with Object.class
    (`List<T> == List<Object>`). Use `context.resolveType(type)` to replace type variables
    with known types before comparison (and use `context.resolveTypeGenerics(type)` to get direct generics of type).

Types may also be checked for compatibility:

```java
Type one = List<Number> // ParameterizedType
Type two = List<Integer> // ParameterizedType

TypeUtils.isAssignable(one, two) == false
// means one assignable to two or two assignable to one
TypeUtils.isCompatible(one, two) == true
// detects type with more specific definition
TypeUtils.isMoreSpecific(one, two) == false
TypeUtils.getMoreSpecific(one, two) == two
```  

If required, you can implement your own logic based on types comparison: see [types walker](../guide/walker.md) section.

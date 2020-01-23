# Find variables

Could be used for checks if type contains variables:

```java
class Base<T> {
    List<T> get() {}
}

// List<T>
Type type = base.class.getDeclaredMethod("get").getGenericReturnType(); 

GenericUtils.findVariables(type) == [<T>] // <T> is TypeVariable instance
```

# Reflection assist

When class is deeply analyzed (often methods or fields processing) context is prepared once 
and navigated to required type in the hierarchy:

```java
// preparing generics context before analysis because it would be impossible
// to resolve correct generics for required class in hierarchy
GenericsContext context = GenericsResolver.resolve(Root);

analyzeType(context, Root.class);

void analyzeType(GenericsContext context) {
    Class type = context.currentClass();
    // do some analysis (e.g. review methods)
    for(Method method: type.getDeclaredMethods()) {
        Class res = context.method(method).resolveReturnClass();
        // collection returned
        if (Iterable.isAssignableFrom(res)) {
            Class collectionType = context.method(method)
                                        .resolveReturnTypeGeneric();
            //...
        }        
    }
    
    // example type resolvution inside class (note there is a shortcut method .resolveFieldClass(),
    // here raw resolution used as an example)
    Class fieldType = context.resolveClass(type.getDeclaredField("smth").getGenericType())
    
    // continue analysis for superclass
    Class superclass = type.getSuperclass();    
    analyzeType(context.type(superclass));
}
```

## Type utils

To avoid dealing with type objects (`ParameterizedType`) context provides pure utility methods 
(not actually tied to "context"):
- `context.resolveTypeGenerics(type)` resolved generics of provided type
- `context.resolveClass(type)` returns raw type's class (reducing type information)
- `context.resolveType(type)` return type with replaced variables

```java
public class Base<T> {
    T field;
}
public class Root extends Base<List<String>> {}

GenericsContext context = GenericsResolver.resolve(Root.class).type(Base.class);
// List<String>
Type fieldType = Base.class.getDeclaredField("field");
// type class
context.resolveClass(fieldType) == List.class;
// first generic class (type information may be reduced)
context.resolveGenericOf(fieldType) == String.class;
// general case (no type precision lost)
context.resolveTypeGenerics(fieldType) == [String.class];
```

!!! tip
    You may use `commons-lang` [ConstructorUtils](https://www.baeldung.com/java-commons-lang-3#the-constructorutils-class), 
    [FieldUtils](https://www.baeldung.com/java-commons-lang-3#the-fieldutils-class) 
    or [MethodUtils](https://www.baeldung.com/java-commons-lang-3#the-fieldutils-class) to simplify fields, methods or 
    constructor searches (for example, if you know field name but don't know exact type in hierarchy).
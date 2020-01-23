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
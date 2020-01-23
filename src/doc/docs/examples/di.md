# DI binding

Universal binding mechanism: suppose we have `Provider<T>` instances and want to bind them (in DI container like guice): 

```java
Provider providerInstance = ...;

Class type = GenericsResolver.resolve(providerInstance.getClass())
        .type(Provider.class)
        .generic(0);

bind(type).toProvider(providerInstance);
```

For example, for instance of `class MyProvider implements Provider<MyType>` binding will be
`bind(MyType.class),toProvider(MyProviderInstance)`

!!! note 
    Guice binding syntax used, just an example to get overall idea.
    
!!! warning
    This will work only if generic was decled for provider class because otherwise
    generic would be resolved as `Object` (if generic not declared - nowhere to get type information)      

## Universal mechanism (for any type)

And here is an example of universal binding mechanism (pseudo DI):

```java
public void bindExtension(Class extensionType, Class contractType) {    
    // actual generics of required extension
    Type[] generics = GenericsResolver.resolve(extensionType)
                                  .type(contractType)
                                  .genericTypes()
                                  .toArray(new Type[0]);
    // actual extension type (specific to provided instance)
    Type bindingType = generics.length > 0 
                    ? new ParameterizedTypeImpl(contractType, generics)
                    : contractType;
    
    bind(bindingType).to(extensionObjectType)
}
```

For example, if we have extension `class MyExtension implements Useful<String>` we can bind it with correct generics 
`bindExtension(MyExtension.class, Useful.class)` which perform `bind(Useful<String>).to(MyExtension.class)`.        

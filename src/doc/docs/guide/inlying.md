# Inlying context

Inlying context is generics context build for type inside current context.

```java
class Root<T> {
    Inlying<T> field;   
}
```

Suppose we analyzing some hierarchy with root and need to build hierarchy for field type.
If we do `GenericsResolver.resolve(Inlying)` then we will lost information about known generic T.

So we need inlying context (lying in current context): 

```java
// note that .type(Root.class) is not required, and used just to show that root
// context contains Root.class 
GenericsContext inlyingContext = context.type(Root.class)
        .fieldType(Root.class.getDeclaredField("field"))
```

Resulted context (for `Inlying`) will contain known value for root generic T.

You can check if current context is inlying by `context.isInlying()` and navigate
to root context using `context.rootContext()`.

!!! note 
    Inlying context also inherits all ignored classes specified during root context 
    creation (`GenericResolver.resolve(Root.class, [classes to ignore])`). 

If target type does not contains generics then type resolution will be cached
(because it is the same as direct type resolution).

## Inlying inner classes

Note: inner class requires outer class instance for creation (differs from [static nested classes](https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html))

```java
class Outer<T> {
    class Inner {
        // inner class use owner class generic 
        T getSomething() {}
    }
}

class Root extends Outer<String> {
    Inner field;
}
```

Inner class could access generics of owner class (T). *In most cases* inner class is used inside
owner class and so if you building inlying context for inner class and
root context contains outer class in hierarchy then **outer class generics used from  root context**

```java
                          // root context
GenericsContext context = GenericsResolver.resolve(Root.class)
    // inlying context
    .fieldType(Root.class.getDeclaredField("field"));

context.ownerGenericsMap() == ["T": String]  // visible generics of Outer
```  

Note that this **assumption** is true not for all cases, but for most.

When outer class generics declared explicitly:

```java
Outer<Long>.Inner field;
```

Explicit declaration is used instead: `context.ownerGenericsMap() == ["T": Long] `.

## Inlying context for sub type

In some (rare) cases, you may need to build inlying context for sub type of declared type:

```java
class Base<T> {
    Inlying<T> field;   
}

class Root extends Base<List<String>> {}

class InlyingExt<K> extends Inlying<List<K>> {}
```

Possible case is object instance analysis when type information could be taken both from 
class declaration and actual values.

```java
GenericsContext context = GenericsResolver.resolve(Root.class)
    .fieldTypeAs(Base.class.getDeclaredField("field"), InlyingExt.class);

// InlyingExt generic tracked from known Inlying<List<String>>
context.genericsMap() == ["K": String]

// context.toString():
// class InlyingExt<String> resolved in context of Root  <-- current
//   extends Inlying<List<String>
```        

## General case

Field cases above are shortcuts for general inlying contexts resolution methods:

```java
context.inlyingType(type)
context.inlyingType(type, asType)
```                              

It may be used to build such context for any type.

For example, it may be required during method parameters (or return type) inspection:

```java
public class Some<K> {
    List<K> fld;
}

public class Base<T> {
    public void something(Some<T> some) {}
}

public class Root extends Base<String> {}
```                                      

Resolvingmethod context for root class:

```java                 
Method method = Base.class.getMethod("something", Some.class);
GenericsContext methodContext = GenericsResolver.resolve(Root.class).method(methiod);
```

If we want to resolve field `fld` type in method parameter type `Some` we need to 
build inlying (sub) context for it under knowing generics context:

```java
GenericsContext paramContext = methodContext.inlyingType(method.getGenericParameterTypes()[0])
paramContext.currentClass() == Some
paramContext.genericsMap() == [K: Strng]
```

Here you can see that parameter context was created with correct generic value.

!!! tip
    This was just an example of raw type context creation (under resolved generic context). 
    Specifically, for methods there is a shortcut method for building parameter context:
    
    ```java
    GenericsContext paramContext = methodContext.parameterType(0)
    ```
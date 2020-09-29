# Getting started

## Installation

[![JCenter](https://img.shields.io/bintray/v/vyarus/xvik/generics-resolver.svg?label=jcenter)](https://bintray.com/vyarus/xvik/generics-resolver/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/generics-resolver.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/generics-resolver)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>generics-resolver</artifactId>
  <version>3.0.3</version>
</dependency>
```

Gradle:

```groovy
implementation 'ru.vyarus:generics-resolver:3.0.3'
```    

Requires java 6 or above (compatible with java 11).

## Usage

!!! note ""
    See [java reflection tutorial](https://www.javacodegeeks.com/2014/11/java-reflection-tutorial-2.html)
    if you have problems with reflection (minimal reflection knowledge is required for usage). 

Suppose we have class hierarchy:

```java
public class Base<T, K> {  
    
  private List<K> fld;   
    
  T doSomething(K arg) {...}
}

public class Root extends Base<Integer, Long> {...}
```

`Base` class generics could only be known in context of extending class (declared generics).
Generics context must be created (from root class):

```java
// compute generics for classes in Root hierarchy
GenericsContext context = GenericsResolver.resolve(Root.class)
        // switch current class to Base (to work in context of it)
        .type(Base.class);
```

!!! hint
    All generics in the root class hierarchy are resolved immediately and resolution data is [cached](guide/cache.md)
    internally, so it is cheap to call generics resolution for the same type in multiple places. 
    
Getting `Base` class generics (in context of `Root`):

```java
context.generics() == [Integer.class, Long.class]
```

Resolving methods:

```java          
//  T doSomething(K arg)
MethodGenericsContext methodContext = context
    .method(Base.class.getMethod("doSomething", Object.class))     

// method return class (in context of Root)
methodContext.resolveReturnClass() == Integer.class

// method parameters (in context of Root)
methodContext.resolveParameters() == [Long.class]
```            

The same way fields, constructors and even direct types could be resolved.

Generics context assumed to be used during reflection introspection to provide additional
types information.

!!! note
    It is important to always properly switch context (with `.type()`) in order to correctly solve types.  
    BUT if you work with fields, methods or constructors, context will be switched automatically.
    For example, field resolution:
    
    ```java
    GenericsResolver.resolve(Root.class)
        .resolveFieldsType(Base.class.getDeclaredField('fld')) == List<Long>
    ```
    
    Note that here we did not do manual context switch to `Base` class, because
    field contains declaration type and so countext could switched automatically. 
    
### Utilities

There are 2 API levels: 

* [Context API](guide/context.md) (primary) used to support introspection (reflection analysis)
and direct utilities. Context API is safe because it always performs compatibility checks and throws descriptive
exceptions. 
* [Static utilities](guide/utils.md). With utilities, usage errors are possible (quite possible to use wrong generics map), but in simple cases 
it's not a problem. Use API that fits best your use case.  

## How to learn

Library is pretty low-level, so it would be easier to first look on usage examples
to find exact cases and only after that go to theory section for details. 

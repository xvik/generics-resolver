# Context API

!!! note "" 
    There are 2 APIs: context API (primary) used to support introspection (reflection analysis)
    and direct [utilities](utils.md). Context API is safe because it always performs compatibility checks and throws descriptive
    exceptions. With utilities, usage errors are possible (quite possible to use wrong generics map), but in simple cases 
    it's not a problem. Use API that fits best your use case. 

Class hierarchy needs to be parsed to properly resolve all generics:

```java
GenericsContext context = GenericsResolver.resolve(Root.class)
```

Now we can perform introspection of any class in the hierarchy (look methods or fields) and know exact generics.

If root class also contains generics, they are resolved by upper generic bound (e.g. `<T extends Model>` will be 
resolved as `T=Model.class`, and resolved as `Object.class` when no bounds set)

!!! note "" 
    Resolved class hierarchy is cached internally, so it's *cheap* to resolve single class many times
    (call `GenericsResolver.resolve(Class)`).

## Limit hierarchy resolution

You can limit hierarchy resolution depth (or exclude some interfaces) by providing ignored classes: 

```java
GenericsResolver.resolve(Root.class, Base.class, SomeInterface.class)
```

Here hierarchy resolution will stop on `Base` class (will not be included) and 
all appeared `SomeInterface` will be skipped (interfaces are also hierarchical so it could exclude sub hierarchy too).  

Option exists for very rare cases when some types breaks analysis (as possible bug workaround).

!!! warning 
    When ignored classes specified, resolved generics information is not cached(!) even if complete type resolution
    was done before (descriptor always computed, but it's not expensive).

## Context

`GenericsResolver.resolve(Class)` returns immutable context (`GenericsContext`) set to root class (by default).

Actually, context is a map (`Map<Class, Map<String, Type>>`) containing generics of all types in hierarchy:

```
class Root                           // [:]
  extends Base<Integer, Long>        // [T:Integer, K:Long]
```

Note that map could also contain visible outer type generics. E.g. for `Outer<A>.Inner<B,C>`
A,B and C will be contained inside type generics map. 

It is very important concept: you will always need to resolve types in context of particular class from
hierarchy and context ties all methods to that class (selects correct generics collection).

To navigate on different class use

```java
context.type(Base.class)
```

Which returns new instance of context. This method is used to navigate through all types in resoled class hierarchy.
Note that new context will use the same root map of generics (it's just a navigation mechanism) and so 
there is no need to remember root context reference: you can navigate from any type to any type inside the resolved hierarchy.

For methods and constructors, which may contain extra generics, generics are resolved in time of method or constructor 
context navigation.

Context operates on types (`Type`), not classes, because only types holds all generics information, including composite
generics info (e.g. `List<List<String>>`). Any type, obtained using reflection may be resolved through api to real class.

!!! important
    See context API methods javadoc: it almost always contains example. Moreover, methods
    are grouped by name to simplify search (you can note `generic..`, `resolve..`, `toString..` groups).  

All classes in root class hierarchy may be obtained like this:

```java
context.getGenericsInfo().getComposingTypes()
```

This will be all classes and interfaces in hierarchy (including root class), even if they not contain generics.

!!! tip
    `toString()` called on context instance returns complete context with current position marker:

`context.type(Base.class).toString()`:

```
class Root                           
  extends Base<Integer, Long>    <-- current
```

For example, in intellij idea, current context (with resolved generics) and position could be seen by using "view value" link inside debugger 

## Class generics

First group of context api methods targets context type own generics. All these methods starts from `generic..`.

For example (in context of `Base` class),

```java
context.genericsMap() == [T: Integer, K: Long] 
```

Returns complete mapping of generic names to resolved types, which may be used to some name based substitution.

```java
context.genericsAsString() == ["Integer", "Long"]
```

Returns string representation of generic types, which may be used for logging or reporting.
If generic value is parameterizable type then string will include it too: `"List<String>"`.

See api for all supported methods.

## Methods

When working with methods it's required to use method context:

```java
MethodGenericsContext methodContext = context.method(methodInstance)
```

This will also check if method belongs to current hierarchy and switch context to 
methods's declaring class (in order to correctly solve referenced generics).

Special method context is important because of method generics, for example:

```java
class A {

  public <T> T method(T arg) {
  ...
  }
}
```

Initially generics are resolved as type, so if you try to analyze generified method parameter it will fail, because
of unknown generic (T).

Method context resolve method generics as upper bound. In the example above it would be: `T == Object`.
But in more complex example:

```java
class A<Q> {

  public <T extends Q, K extends Cloneable> T method(T arg, List<K> arg2) {
  ...
  }
}

class B extends A<Serializable>
```

Method generics upper bounds could be resolved as:

```java
Method method = A.getMethod("method", Object.class, Cloneable.class)
GenericsResolver.resolve(B.class).method(method)
    .methodGenericTypes() == ["T": Serializable.class, "K": Cloneable.class]
```

Method context additional methods to work with parameters and return type:

```java
methodContext.resolveParameters() == [Serializable.class, List.class]
methodContext.resolveReturnClass() == Serializable.class
```

Types resolution api (the same as in types context) will count method generics too:

```java
methodContext.resolveGenericOf(method.getGenericParameterTypes()[1]) == Cloneable.class
```

## Constructors

Constructor could declare generics like:

```java
class Some {
    <T> Some(T arg);
}
```

To work with constructor generics, constructor context must be created:

```java
ConstructorGenericsContext ctorContext = context.constructor(Some.class.getConstructor(Object.class))
```

By analogy with method context, constructor context contains extra methods for working
with constructor generics and parameters. 

## Fields

Context contains special shortcut methods for working with fields. For example,

```java
context.resolveFieldClass(field)
```

In essence, it's the same as: `context.resolveClass(field.getGenericType())`

It is just shorter and, if field does not belongs to current hierarchy, more concrete error will be thrown.
But it would be IllegalArgumentException instead of WrongGenericsContextException because
field case assumed to be simpler to track and more obvious to predict. 

## Types resolution

Both `MethodGenericContext` and `ConstructorGenericContext` extends from `GenericsContext` and so share common api.
The only difference is that in method and context contexts amount of known generics could be bigger (due to method/constructor generics).

All type resolution api methods starts with 'resolve..'.

This api most likely will be used together with reflection introspection of classes in hierarchy (e.g.
when searching for method or need to know exact method return type).

Any `Type` could be resolved to actual class (simpler to use in logic) and manual navigation
to actual context type is not required.

Suppose we have more complex case:

```java
class Base<T, K extends Collection<T> {
  K foo;
}

class Root extends Base<Integer, List<Integer>> {...}
```

And we need to know type and actual type of collection in field `foo`:

```groovy
Field field = Base.class.getField("foo")
GenericsContext context = GenericsResolver.resolve(Root.class)
        // this is optional step (result will be the same even without it)
        .type(Base.class)
context.resolveClass(field.getGenericType()) == List.class
context.resolveGenericOf(field.getGenericType()) == Integer.class
```

Here you can see how both main class and generic class resolved from single type instance.

See api for all supported methods.

Note that type navigation (`.type()`) is important when you need to access exact type
generics. For example, in order to use type's generics map in direct utility calls.

## To string

```java
class Base<T, K> {
  T doSomething(K arg);
}

class Root extends Base<Integer, Long> {...}
```

Any type could be resolved as string:

```java
context.toStringType(doSomethingMethod.getGenericReturnType()) == "List<Integer>"
```

Or context class:

```java
context.type(Base.class).toStringCurrentClass() == "Base<Integer, Long>"; 
context.type(Base.class).toStringCurrentClassDeclaration() == "Base<T, K>"
```

To string context method:

```java
context.method(doSomethingMethod).toStringMethod() == "Integer doSomething(Long)"
```

By analogy, constructor context also contains `toStringConstructor()` method.

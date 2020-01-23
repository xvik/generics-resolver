# Usage

!!! note 
    There are 2 APIs: context API (primary) used to support introspection (reflection analysis)
    and direct utilities. Context API is safe because it always performs compatibility checks and throws descriptive
    exceptions. With utilities, usage errors are possible (quite possible to use wrong generics map), but in simple cases 
    it's not a problem. Use API that fits best your use case. 

Class hierarchy needs to be parsed to properly resolve all generics:

```java
GenericsContext context = GenericsResolver.resolve(Root.class)
```

Now we can perform introspection of any class in hierarchy (look methods or fields) and know exact generics.

If root class also contains generics, they are resolved by upper generic bound (e.g. `<T extends Model>` will be 
resolved as `T=Model.class`, and resolved as `Object.class` when no bounds set)

NOTE: Resolved class hierarchy is cached internally, so it's *cheap* to resolve single class many times
(call `GenericsResolver.resolve(Class)`).

## Partial hierarchy

You can limit hierarchy resolution depth (or exclude some interfaces) by providing ignored classes: 

```java
GenericsResolver.resolve(Root.class, Base.class, SomeInterface.class)
```

Here hierarchy resolution will stop on `Base` class (will not be included) and 
all appeared SomeInterfaces will be skipped (interfaces are also hierarchical so it could exclude sub hierarchy too).  

Option exists for very rare cases when some types breaks analysis (possible bug workaround).

WARNING: When ignored classes specified, resolved generics information is not cached(!) even if complete type resolution
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

All classes in root class hierarchy may be obtained like this:

```java
context.getGenericsInfo().getComposingTypes()
```

This will be all classes and interfaces in hierarchy (including root class), even if they not contain generics.

IMPORTANT: context class toString() returns complete context with current position marker:

`context.type(Base.class).toString()`:

```
class Root                           
  extends Base<Integer, Long>    <-- current
```

For example, in intellij idea, current context (with resolved generics) and position could be seen by using "view value" link inside debugger 

## Context class generics

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

## Working with methods

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

## Working with constructors

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

## Working with fields

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

## Inlying context

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

### Inlying inner classes

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

### Inlying context for sub type

In some (rear) cases, you may need to build inlying context for sub type of declared type:

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

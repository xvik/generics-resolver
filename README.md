#Java generics runtime resolver
[![Gitter](https://badges.gitter.im/Join%20Chat.svg)](https://gitter.im/xvik/generics-resolver)
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/generics-resolver.svg?style=flat&branch=master)](https://travis-ci.org/xvik/generics-resolver)
[![Coverage Status](https://img.shields.io/coveralls/xvik/generics-resolver.svg?style=flat)](https://coveralls.io/r/xvik/generics-resolver?branch=master)

### About

Suppose common situation:

```java
class Base<T, K> {
  T doSomething(K arg);
}

class Root extends Base<Integer, Long> {...}
```

If you ever need to know exact type of T and K then this library is for you.

```groovy
GenericsContext context = GenericsResolver.resolve(Root.class).type(Base.class)
context.generics() == [Integer.class, Long.class]

Method doSomething = Base.class.getMethod("doSomething", Object.class)
MethodGenericsContext methodContext = context.method(doSomething)
methodContext.resolveReturnClass() == Integer.class
methodContext.resolveParameters() == [Long.class]
```

Features:
* Resolves generics for hierarchies of any depth (all subclasses and interfaces on any level)
* Supports composite generics
* Supports method generics
* Context based api to supplement reflection introspection (to see all possible type information in runtime)
* To string types converter (useful for logging/reporting)

Library was originally written for [guice-persist-orient](https://github.com/xvik/guice-persist-orient) to support
repositories analysis.

### Setup

Releases are published to [bintray jcenter](https://bintray.com/bintray/jcenter) (package appear immediately after release) 
and then to maven central (require few days after release to be published). 

[![Download](https://api.bintray.com/packages/vyarus/xvik/generics-resolver/images/download.svg) ](https://bintray.com/vyarus/xvik/generics-resolver/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/generics-resolver.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/generics-resolver)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>generics-resolver</artifactId>
  <version>2.0.1</version>
</dependency>
```

Gradle:

```groovy
compile 'ru.vyarus:generics-resolver:2.0.1'
```

##### Snapshots

You can use snapshot versions through [JitPack](https://jitpack.io):

* Go to [JitPack project page](https://jitpack.io/#xvik/generics-resolver)
* Select `Commits` section and click `Get it` on commit you want to use (top one - the most recent)
* Follow displayed instruction: add repository and change dependency (NOTE: due to JitPack convention artifact group will be different)

### Usage

Class hierarchy needs to be parsed to properly resolve all generics:

```java
GenericsContext context = GenericsResolver.resolve(Root.class)
```

If root class also contains generics, they are resolved by generic bound (e.g. `<T extends Model>` will be resolved as T=Model.class, and
resolved as Object.class when no bounds set)

Resolved class hierarchy is cached internally, so it's cheap to resolve single class few times.

In some rare cases you may need to exclude some interfaces from resolution (e.g. single interface used with
 different generics) or to limit resolution depth. To do it simply specify classes to ignore just after root class:

```java
GenericsResolver.resolve(Root.class, Callable.class, Runnable.class)
```

When ignored classes specified, resolved generics information is not cached(!) even if complete type resolution
was done before (descriptor always computed).

#### Context

Resolver returns context object (immutable), which is set to root class.
Context itself is very important concept: you will always need to resolve types of particular class from
hierarchy and context ties all methods to that class.

To navigate on different class use

```java
context.type(Base.class)
```
Which returns new instance of context. This method is not tied to actual class hierarchy so you can obtain context
of any class (available in root class hierarchy) from any context instance.

Context operates on types (`Type`), not classes, because only types holds all generics information, including composite
generics info (e.g. `List<List<String>>`). Any type, obtained using reflection may be resolved through api to real class.

All classes in root class hierarchy may be obtained like this:

```java
context.getGenericsInfo().getComposingTypes()
```

This will be all classes and interfaces in hierarchy (including root class), even if they not contain generics.

#### Obtaining class generics

First group of context api methods targets context type own generics. All these methods starts from 'generic..'.

For example,

```java
context.genericsMap()
```

Returns complete mapping of generic names to resolved types, which may be used to some name based substitution.

```java
context.genericsAsString()
```

Returns string representation of generic types, which may be used for logging or reporting.

See api for all supported methods.

#### Working with methods

When working with methods its required to use method context:

```java
MethodGenericsContext methodContext = context.method(methodInstance)
```

Special method context is important because of method generics, for example:

```java
class A {

  public <T> T method(T arg) {
  ...
  }
}
```

Initially generics are resolved for type, so if you try to analyze generified method parameter it will fail, because
of unknown generic.

Method context resolve method generics as lower bound. In the example above it would be: T == Object.
But in more complex example:

```java
class A<Q> {

  public <T extends Q, K extends Cloneable> T method(T arg, List<K> arg2) {
  ...
  }
}

class B extends A<Serializable>
```

Method generics lower bounds could be resolved:

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

Types resolution api (the same as int types context) will count method generics too:

```java
methodContext.resolveGenericOf(method.getGenericParameterTypes()[1]) == Cloneable.class
```

Important moment: when navigating to method context, context type is set automatically to 
method declaring class. This is important to properly resolve references to class generics.

#### Types resolution

Both `MethodGenericContext` and `TypeGenericContext` extends from `GenericsContext` and so share type resolution api.
The only difference is that in method context amount of known generics could be bigger (due to method generics).

Context api methods group targets low level types resolution. All these methods starts from 'resolve..'.

This api most likely will be used together with reflection introspection of classes in hierarchy (e.g.
when searching for method or need to know exact method return type).

When context tied to class, it is able to solve any `Type` to actual class.

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
GenericsContext context = GenericsResolver.resolve(Root.class).type(Base.class)
context.resolveClass(field.getGenericType()) == List.class
context.resolveGenericOf(field.getGenericType()) == Integer.class
```

Here you can see how both main class and generic class resolved from single type instance.

See api for all supported methods.

#### To string

Any type could be resolved as string:

```groovy
context.toStringType(doSomething.getGenericReturnType()) == "List<Integer>"
```

### Cache

If you use JRebel or other class reloading tool (maybe some other reason) you will need to disable descriptors caching.

To do it set system property or environment variable:

```
ru.vyarus.java.generics.resolver.context.GenericsInfoFactory.cache=false
```

Or from code:

```java
GenericsInfoFactory.disableCache();
```

Also you can clear cache manually:

```java
GenericsInfoFactory.clearCache()
```

### Use cases

Few real life usages.

[guice-persist-orient](https://github.com/xvik/guice-persist-orient) use it to introspect finder types hierarchies.
Before generics resolution, finders were flat, because internal logic relies on method return types.
Another example is searching for target method using arguments: argument generics resolution greatly reduce search scope.
And the last example is using generic name to substitute correct type in sql query, which allows to write completely generic queries.

[dropwizard-guicey](https://github.com/xvik/dropwizard-guicey) use it for reporting of usage examples for registered jersey plugins
(resolving plugin interface generics, convert them to strings and compose example report).
Another aspect is factories registration: for proper jersey integration it needs to know types for registered factories (`Factory<T>` class).

### Supplement

[reflection tutorial](http://www.javacodegeeks.com/2014/11/java-reflection-api-tutorial.html)

-
[![java lib generator](http://img.shields.io/badge/Powered%20by-%20Java%20lib%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-lib-java)

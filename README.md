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
Method doSomething = Base.class.getMethod("doSomething", Object.class)
GenericsContext context = GenericsResolver.resolve(Root.class).type(Base.class)
context.generics() == [Integer.class, Long.class]
context.resolveReturnClass(doSomething) == Integer.class
context.resolveParameters(doSomething) == [Long.class]
```

Features:
* Resolves generics for hierarchies of any depth (all subclasses and interfaces on any level)
* Supports composite generics
* Context based api to supplement reflection introspection (to see all possible type information in runtime)
* To string types converter (useful for logging/reporting)

Library was originally written for [guice-persist-orient](https://github.com/xvik/guice-persist-orient) to support
finders analysis.

### Setup

Releases are published to [bintray jcenter](https://bintray.com/bintray/jcenter) (package appear immediately after release) 
and then to maven central (require few days after release to be published). 

[![Download](https://api.bintray.com/packages/vyarus/xvik/generics-resolver/images/download.svg) ](https://bintray.com/vyarus/xvik/generics-resolver/_latestVersion)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/generics-resolver/badge.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/generics-resolver)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>generics-resolver</artifactId>
  <version>1.1.0</version>
</dependency>
```

Gradle:

```groovy
compile 'ru.vyarus:generics-resolver:1.1.0'
```

### Usage

Class hierarchy needs to be parsed to properly resolve all generics:

```java
GenericsContext context = GenericsResolver.resolve(Root.class)
```

If root class also contains generics, they will not be resolved (it's impossible).

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

This will be all classes (and interfaces) in hierarchy, even if they not contain generics.

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

#### Types resolution

Other context api methods group targets low level types resolution. All these methods starts from 'resolve..'.

This api most likely will be used together with reflection introspection of classes in hierarchy (e.g.
when searching for method or need to know exact method return type).

When context tied to class, it is able to solve any `Type` to actual class.

Suppose we have more complex case:

```java
class Base<T, K extends Collection<T> {
  K doSomething();
}

class Root extends Base<Integer, List<Integer>> {...}
```

And we need to know method return type and actual type of collection:

```groovy
Method doSomething = Base.class.getMethod("doSomething")
GenericsContext context = GenericsResolver.resolve(Root.class).type(Base.class)
context.resolveClass(doSomething.getGenericReturnType()) == List.class
context.resolveGenericOf(doSomething.getGenericReturnType()) == Integer.class
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

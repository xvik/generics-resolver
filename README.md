# Java generics runtime resolver
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/generics-resolver.svg?style=flat&branch=master)](https://travis-ci.org/xvik/generics-resolver)
[![Appveyor build status](https://ci.appveyor.com/api/projects/status/github/xvik/generics-resolver?svg=true)](https://ci.appveyor.com/project/xvik/generics-resolver)
[![codecov](https://codecov.io/gh/xvik/generics-resolver/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/generics-resolver)

**DOCUMENTATION** https://xvik.github.io/generics-resolver

### About

```java
class Base<T, K> {
  T doSomething(K arg);
}

class Root extends Base<Integer, Long> {...}
```

Library was created to support reflection analysis (introspection) with all available types information.

```java
// compute generics for classes in Root hierarchy
GenericsContext context = GenericsResolver.resolve(Root.class)
        // switch current class to Base (to work in context of it)
        .type(Base.class);

context.generics() == [Integer.class, Long.class]

MethodGenericsContext methodContext = context
    .method(Base.class.getMethod("doSomething", Object.class))
// method return class (in context of Root)
methodContext.resolveReturnClass() == Integer.class
// method parameters (in context of Root)
methodContext.resolveParameters() == [Long.class]
```

Features:
* Resolves generics for hierarchies of any depth (all subclasses and interfaces on any level)
* Supports 
    - composite generics (e.g. `Smth<T, K extends List<T>>`)
    - method generics (`<T> T getSmth()`)
    - constructor generics (`<T> Some(T arg)`)
    - outer class generics (`Outer<T>.Inner`)
* Context api completely prevents incorrect generics resolution (by doing automatic context switching)
* Sub contexts: build context from Type in current context to properly solve root generics  
* Generics backtracking: track higher type generics from some known middle type 
* To string types converter (useful for logging/reporting)
* General types comparison api (assignability, compatibility, specificity checks)

NOTE: Java 8 lambdas are *not supported* because there is no official way to analyze lambdas 
due to [implementation](http://mail.openjdk.java.net/pipermail/compiler-dev/2015-January/009220.html).
It is [possible](https://github.com/jhalterman/typetools) to use [some hacks to resolve lambda geneics](https://stackoverflow.com/a/25613179/5186390) in some cases,
but it's quite fragile (may break on future java releases or not work on other java implementations).  

Library targets actual classes analysis and, personally, I never really need to analyze lambdas. 

Library was originally written for [guice-persist-orient](https://github.com/xvik/guice-persist-orient) to support
repositories analysis and later used in [dropwizard-guicey](https://github.com/xvik/dropwizard-guicey) for extensions analysis.

Compatible with Java 6 and above.

##### Alternatives

For simple cases (e.g. to resolve class/interface generic value), look, maybe you already 
have required tool in the classpath (and it will be enough):
  
* Guava [TypeToken](https://github.com/google/guava/wiki/ReflectionExplained#typetoken)
* Spring [GenericTypeResolver](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/GenericTypeResolver.html)
* Commons-lang3 [TypeUtils](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/reflect/TypeUtils.html) 

### Setup

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
compile 'ru.vyarus:generics-resolver:3.0.3'
```

#### Snapshots

<details>
      <summary>Snapshots may be used through JitPack</summary>
      
* Go to [JitPack project page](https://jitpack.io/#ru.vyarus/generics-resolver)
* Select `Commits` section and click `Get it` on commit you want to use (top one - the most recent)
* Follow displayed instruction: 
    - Add jitpack repository: `maven { url 'https://jitpack.io' }`
    - Use commit hash as version: `ru.vyarus:generics-resolver:56537f7d23` (or use `master-SNAPSHOT`)
    
</details>   

### Usage

Read [documentation](https://xvik.github.io/generics-resolver/)     
      
---
[![java lib generator](http://img.shields.io/badge/Powered%20by-%20Java%20lib%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-lib-java)

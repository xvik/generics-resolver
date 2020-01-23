# Welcome to generics-resolver

!!! summary ""
    Java generics runtime resolver

[Release notes](about/history.md) - [Support](about/support.md) - [License](about/license.md)

Resolves declared generics within class hierarchies in order to provide all 
available type information at runtime.

!!! note 
    Java 8 lambdas are *not supported* because there is no official way to analyze lambdas 
    due to [implementation](http://mail.openjdk.java.net/pipermail/compiler-dev/2015-January/009220.html).
    It is [possible](https://github.com/jhalterman/typetools) to use [some hacks to resolve lambda geneics](https://stackoverflow.com/a/25613179/5186390) in some cases,
    but it's quite fragile (may break on future java releases or not work on other java implementations).
    
    Library targets actual classes analysis and, personally, I never really need to analyze lambdas.  
 
Library was originally written for [guice-persist-orient](https://github.com/xvik/guice-persist-orient) to support
repositories analysis and later used in [dropwizard-guicey](https://github.com/xvik/dropwizard-guicey) for extensions analysis.

Compatible with Java 6 and above.                 

## Main features

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

## Alternatives

For simple cases (e.g. to resolve class/interface generic value), look, maybe you already 
have required tool in the classpath (and it will be enough):
  
* Guava [TypeToken](https://github.com/google/guava/wiki/ReflectionExplained#typetoken)
* Spring [GenericTypeResolver](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/GenericTypeResolver.html)
* Commons-lang3 [TypeUtils](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/reflect/TypeUtils.html)
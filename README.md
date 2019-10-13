# Java generics runtime resolver
[![License](http://img.shields.io/badge/license-MIT-blue.svg?style=flat)](http://www.opensource.org/licenses/MIT)
[![Build Status](http://img.shields.io/travis/xvik/generics-resolver.svg?style=flat&branch=master)](https://travis-ci.org/xvik/generics-resolver)
[![codecov](https://codecov.io/gh/xvik/generics-resolver/branch/master/graph/badge.svg)](https://codecov.io/gh/xvik/generics-resolver)

Support: [gitter chat](https://gitter.im/xvik/generics-resolver) 

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

[2.0.1 version documentation](https://github.com/xvik/generics-resolver/tree/2.0.1)

##### Alternatives

For simple cases (e.g. to resolve class/interface generic value), look, maybe you already 
have required tool in the classpath (and it will be enough):
  
* Guava [TypeToken](https://github.com/google/guava/wiki/ReflectionExplained#typetoken)
* Spring [GenericTypeResolver](https://docs.spring.io/spring-framework/docs/current/javadoc-api/org/springframework/core/GenericTypeResolver.html)
* Commons-lang3 [TypeUtils](https://commons.apache.org/proper/commons-lang/apidocs/org/apache/commons/lang3/reflect/TypeUtils.html) 

### Setup

Releases are published to [bintray jcenter](https://bintray.com/bintray/jcenter) and maven central. 

[![Download](https://api.bintray.com/packages/vyarus/xvik/generics-resolver/images/download.svg) ](https://bintray.com/vyarus/xvik/generics-resolver/_latestVersion)
[![Maven Central](https://img.shields.io/maven-central/v/ru.vyarus/generics-resolver.svg?style=flat)](https://maven-badges.herokuapp.com/maven-central/ru.vyarus/generics-resolver)

Maven:

```xml
<dependency>
  <groupId>ru.vyarus</groupId>
  <artifactId>generics-resolver</artifactId>
  <version>3.0.1</version>
</dependency>
```

Gradle:

```groovy
compile 'ru.vyarus:generics-resolver:3.0.1'
```

##### Snapshots

You can use snapshot versions through [JitPack](https://jitpack.io):

* Go to [JitPack project page](https://jitpack.io/#xvik/generics-resolver)
* Select `Commits` section and click `Get it` on commit you want to use (top one - the most recent)
* Follow displayed instruction: add repository and change dependency (NOTE: due to JitPack convention artifact group will be different)

### Examples (practice)

#### Type resolution safety

```java
class Base<T> {
    private T field;
    
    public <K extends Comparable> K get(){}
}

// generic with the same name
class Base2<T> extends Base<Long> {
    private T field;
}

class Root extends Base2<String> {}

// not in Root hierarchy
class NotInside<T> {
    private List<T> field;
}
```

After context resolution, any generics, containing in type hierarchy would be properly
resolved:

```java
// note context class is Root
GenericsContext context = GenericsResolver.resolve(Root.class)

// context.toString():
// class Root    <-- current
//   extends Base2<String>
//     extends Base<Long>

// automatically detected context Base and use correct generics
context.resolveClass(Base.class.getDeclaredField("field").getGenericType()) == Long.class
// Base2 recognized and appropriate generic used
context.resolveClass(Base2.class.getDeclaredField("field").getGenericType()) == String.class
// method context automatically detected and type properly resolved
context.resolveClass(Base.class.getMethod("get").getGenericReturnType())== Comparable.class
```

But, when generics in type belongs to different hierarchy:

```java
context.resolveClass(NotInside.class.getDeclaredField("field").getGenericType())
```

exception is thrown:

```
ru.vyarus.java.generics.resolver.error.WrongGenericsContextException: Type List<T> contains generic 'T' (defined on NotInside<T>) and can't be resolved in context of current class Root. Generic does not belong to any type in current context hierarchy:
class Root
  extends Base2<String>
    extends Base<Long>

	at ru.vyarus.java.generics.resolver.context.GenericsContext.chooseContext(GenericsContext.java:233)
	at ru.vyarus.java.generics.resolver.context.AbstractGenericsContext.resolveClass(AbstractGenericsContext.java:273)
```

#### Reflection assist

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

#### Types sanitizer

You may already have some api for working with `Type` objects.
In this case resolver could be used to replace all generic variables with actual types:

```java
class Base<T> {
    List<T> field;    
}

class Root extends Base<Integer> {}
```

```java
GenericsContext context = GenericsResolver.resovle(Root.class);

// pure reflection, actual type is List<T>
Type fieldType = Base.class.getField("field");
// sanitize type to List<Integer>
fieldType = context.type(Base.class).resovleType(fieldType);

// continue working with pure type
```

Alternatively, if class hierarchy is not known and we want to remove
generics in context of current class only

```java
Type fieldType = GenericsUtils
            .resolveTypeVariables(fieldType, new IgnoreGenericsMap())
```

Will resolve type as `List<Object>` (unknown generic "T" resolved as Object.class). 

Sometimes it is useful to extract generics of type:

```java
// type's generics
Type[] typeGenerics = GenericsUtils.getGenerics(type, generics);
// type's with replaced variables 
Type[] sanitizedGenerics = GenericsUtils
            .resolveTypeVariables(typeGenerics, generics); 
``` 

Note: Generics map (generics) of host type could be taken form host type's generics context (`context.visibleGenericsMap()`) 
or manually (see generics map direct resolution example below).

For example, if original type was `Map<String, List<T>>` then sanitizedGenerics will be
`[String, List<Integer>]` (suppose T is Integer in current context).

#### Base type generics resolution (implemented interface or extended class)

Common case for various extension mechanisms is to know actual generics of 
some extension interface for actual extension class:
 
```java
interface Extension<V> {}

class ListExtension implements Extension<List<String>> {}
```

```java
GenericsResolver.resolve(ListExtension.class)
        .type(Extension.class)
        .genericType("V") == List<String> // (ParameterizedType) 
```

#### Bind extension in DI

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

Note: guice binding syntax used, just an example to get overall idea.

##### Universal mechanism (for any type)

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

#### Work with type's hierarchy

Get all types in class hierarchy (classes and interfaces)

```java
GenericsResolver.resove(Root).getGenericsInfo().getComposingTypes() == 
                                [Root.class, Base.class]
```

Print hierarchy:

```java
GenericsResolver.resove(Root).getGenericsInfo().toString();
```

```
class Root
  extends Base<Integer, Long>
``` 

#### Types compatibility checks

Suppose you have some class:

```java
class Conf {
    SubConf<List<String>> sub; 
}
```

And some other class:

```java
class Listener {
    listen(SubConf<List<Integer>> obj);
}
```

And you want to check if field value from `Conf` could be used in listener:

```java
// for more complex cases there may be generics resolution too 
// pure reflection just for example's simplicity
Type listenerArg = Listener.class
                        .getMethod("listen", SubConf.class)
                        .getGenericParameterTypes()[0];
Type confField = Conf.getField("sub").getGenericType();

// not assignable because of list generics: String vs Integer
TypeUtils.isAssignable(confField, listnerArg) == false    
``` 

NOTE: possible name variables (not resolved) in types are replaced with Object.class
(`List<T> == List<Object>`). Use `context.resolveType(type)` to replace type variables
with known types before comparison (and use `context.resolveTypeGenerics(type)` to get direct generics of type).

Types may also be checked for compatibility:

```java
Type one = List<Number> // ParameterizedType
Type two = List<Integer> // ParameterizedType

TypeUtils.isAssignable(one, two) == false
// means one assignable to two or two assignable to one
TypeUtils.isCompatible(one, two) == true
// detects type with more specific definition
TypeUtils.isMoreSpecific(one, two) == false
TypeUtils.getMoreSpecific(one, two) == two
```  

If required, you can implement your own logic based on types comparison: see TypesWalker section.

#### Generics tracking

```java
class Base<T> {
    
}

class Root<K, P extends Comparable> extends Base<K[]> {}
```

Tracking sub class generics from current (knowing generics):

```java
GenericsTrackingUtils.track(Root, Base, ["T": String[]]) 
                        == ["K": String, "P": Comparable]
```

Not trackable generics resolved from generic declaration (as declared upper bound).

#### Find variables in type

Could be used for checks if type is completely resolved or not:

```java
class Base<T> {
    List<T> get() {}
}

// List<T>
Type type = base.class.getDeclaredMethod("get").getGenericReturnType(); 

GenericUtils.findVariables(type) == [<T>] // <T> is TypeVariable instance of T
```

#### Construct custom types

Custom type containers (used internally for types repackaging) may be used for 
constructing types:

```java
new ParameterizedTypeImpl(List.class. String.class) // List<String>
WildcardTypeImpl.upper(String.class) // ? extends String
WildcardTYpeImpl.lower(String.class) // ? super String
new GenericArrayTypeImpl(String.class) // String[]
```

Note that upper bounded wildcard constructor allows multiple upper bounds, for example: 

```java
WildcardTypeImpl.upper(Number.class, Comparable.class)
``` 
which creates impossible (to declare in java) wildcard `? extends Number & Comparable`. This is used internally for 
multiple bounded variable declaration repackage (which could contain multiple bounds) : `T extends Number & Comparable`.
But, for example, groovy allows multiple bounds in wildcards.

Custom type container could be used in toString logic:

```java
TypeToStringUtils.toStringType(
        new ParameterizedTypeImpl(List.class. String.class),
        Collections.emptyMap()) == "List<String>";
```

#### Direct generics resolution

```java
class Base<T extends Number> {}

class Root extends Base<Integer> {}
```

Get maps of generics in hierarchy:

```java
GenericsResolutionUtils.resolve(Root.class) == [Root.class : [], Base.class: ["T": Integer.class]];
``` 

Or just resolve generics resolution from definition:

```java
GenericsResolutionUtils.resolveRawGenerics(Base.class) == ["T": Number.class ];
```

Or, if generics already known as List, it could be converts to map:

```java
Map<String, Type> generics = GenericsUtils.createGenericsMap(Some.class, knownGenericsList);
``` 

For example, we know that `class MyType<T, K>` has `[String.class, Integer.class]` generics.
Then we can convert it to map: 

```java
GenericsUtils.createGenericsMap(MyType.class, [String.class, Integer.class]) 
                == ["T": String.class, "K": Integer.class]
```

And use resulted map for utility calls (e.g. `GenericsUtils` or `ToStringUtils`).

#### Direct utilities usage

In most cases, to use utility directly, all you need to know is type's generics map.
It could be either obtained from context (`context.type(Some.class).visibleGenericsMap()`)
or resolved directly (as shown above). 

```java
class Some<T> {
    T field;
}
```

Suppose you have generics resolved as `generics` variable.

```java
Map<String, Type> generics = ["T": List<String>];
```

Note that this map must include all visible generics (including outer class, method or constructor if they 
could appear in target types), so better use context api to obtain complete generics map (otherwise UnkownGenericsException could arise at some point).

```java
Type field = Some.class.getDeclaredField("field");
GenericsUtils.resolveClass(field, generics) == List.class;
GenericsUtils.resolveGenericsOf(field, generics) == String.class;

TypeToStringUtils.toStringType(field, generics) == "List<String>";
TypeToStringUtils.toStringWithNamedGenerics(field) == "List<T>";
TypeToStringUtils.toStringWithGenerics(Some.class, generics) == "Some<List<String>>";
```

See context api implementation for more utilities cases (context use utilities for everything).

#### "Inside" type analysis

Suppose you have have hierarchical pojo (e.g. configuration class):

```java
class Conf {
    SubConf<String> dbConf;
}
```

You want to analyze dbConf's type (`SubConf<String>` in context of `Conf`):

```java
// build context for root class
GenericsContext context = GenericsResolver.resove(Conf.class)
            // build sub context for field type
            .fieldType(Conf.getDeclaredField("dbConf"));

// working in sub context as in usual context
context.currentClass() == SubConf.class
context.generics() == [String.class]
...
```   

#### Instance analysis

NOTE: this is very specific case

Suppose you want to analyze current object *instance* structure.
For example, you have configuration object and
you want to compute it's values as paths:

```java
class Conf {
    Base<String> sub;
}
```

```java
Conf conf = ...// instance
GenericsContext context = GenericsResolver.resolve(conf.getClass())

List<PropertyPath> configPaths = new ArrayList<>();
extractConfigPaths(configPaths, "", context)

void extractConfigPaths(Map<String, Object> paths, 
                         String prefix, 
                         GenericsContext context) {
    Class type = context.currentClass();
    // possible sub clas fields ignored for simplicity
    for(Field field: type.getDeclaredFields()) {
        Object value = field.getValue(); // simplification!
        
        // in real life type analysis logic will be more complex 
        // and the most specific type resolution would be required
        Class fieldType = context.resolveFieldClass(field);
        String fieldPath = prefix+"."+field.getName();
        // simplification: primitives considered as final value, 
        // others as pojos to go deeper
        if (fieldType.isPrimitive()) {
            paths.put(fieldPath, value); 
        } else {
            extractConfigPaths(paths, fieldPath,
                        // simplification: value not checked for null
                        context.fieldTypeAs(field, value.getClass()));
        }
    }
}
```

Pay attention to: `context.fieldTypeAs(field, value.getClass())`. 
Class `Conf` declares field as `Base<String> sub`, but actually there might be
more specific type (as we have object instance, we know exact type for sure (`value.getClass() == Specific.class`)):

```java
class Specific<T, K> extends Base<K> {
    T unknown;
    K known;
}
```

To properly introspect fields of `Specific` we need to build generics context for it,
but we know generics only for it's base class (`Base<String>`).
`context.fieldTypeAs` will:
* Track generics declaration from root class and resolve one generic: `Specific<Object, String>`
* Build new context for `Specific<Object, String>`.

With it we can partially know actual field types:

```
"sub.unknown", Object.class
"sub.known", String.class 
```

(without root analysis they both would be recognized as Object.class)

Note that even if specific class generics are not trackable (`class Specific2 extends Base` - generics not trackable),
resolved hierarchy will still use known generic values for sub hierarchy, starting from class with known generics 
(because we know they are correct for sure):

```
class Specific2
    extends Base<String>
```    

#### Generics visibility

```java
public class Outer<A, B, C> {        

    // constructor generic hides class generic C            
    public <C> Outer(C arg){}

    // method generic hides class generic A    
    public <A> A doSmth() {} 
    
    // inner class hides outer generic (can't see)
    public class Inner<A, T> {

            // method generic hides outer class generic
            public <B> B doSmth2() {}        
    }
}

public class Root extends Outer<String, Integer, Long> {
    
    // field with inner class
    Inner<Boolean, String> field1;

    // field with inner class, but with direct outer generics definition    
    Outer<String, String, String>.Inner<Boolean, String> field2;
} 
```

```java
GenericsContext context = GenericsResolver.resolve(Root.class);
```

##### Type generics

```java
// no generics declared on Root class
context.genericsMap() == [:]

// context.toString():
// class Root    <-- current
//   extends Outer<String, Integer, Long>

// Outer type generics, resolved from Root hierarchy
context.type(Outer.class)
    .genericsMap() == ["A": String, "B": Integer, "C": Long]

// context.type(Outer.class).toString():
// class Root
//   extends Outer<String, Integer, Long>    <-- current
```

##### Constructor generics

```java
// switch context to constructor
context = context.constructor(Outer.class.getConstructor(Object.class))
// generics map shows generics of context type (Outer)!
context.genericsMap() == ["A": String, "B": Integer, "C": Long]
context.constructorGenericsMap() == ["C": Object]
// but actually visible generics are (note that constructor generic C override):
context.visibleGenericsMap() == ["A": String, "B": Integer, "C": Object]

// context.toString():
// class Root
//   extends Outer<String, Integer, Long>
//     Outer(Object)    <-- current
```

##### Method generics

```java
// switch context to method
context = context.method(Outer.getMethod("doSmth"))

// generics map shows generics of context type (Outer)!
context.genericsMap() == ["A": String, "B": Integer, "C": Long]
context.methodGenericsMap() == ["A": Object]
// but actually visible generics are (note that method generic A override):
context.visibleGenericsMap() == ["A": Object, "B": Integer, "C": Long]

// context.toString():
// class Root
//   extends Outer<String, Integer, Long>
//     Object doSmth()    <-- current
```


##### Outer class generics

```java
// build context for type of field 1 (using outer generics knowledge)
context = context.fieldType(Root.getDeclaredField("field1"))

context.genericsMap() == ["A": Boolean, "T": String]
// inner class could use outer generics, so visible outer generics are also stored,
// but not A. as it could never be used in context of this inner class
context.ownerGenericsMap() == ["B": Integer, "C": Long]
context.visibleGenericsMap() == ["A": Boolean, "T": String, "B": Integer, "C": Long]

// context.toString()
// class Outer<Object, Integer, Long>.Inner<Boolean, String>  resolved in context of Root    <-- current
```

NOTE: here was an assumption that as Root context contains `Outer` class (outer for `Inner`), then inner class was created inside it
and so root generics could be used. It is not always the case, but in most cases inner class is used inside of outer.

Different case, when outer class generics are explicitly declared:

```java
// build context for type of field 2 (where outer generics are explicitly declared)
// note that first we go from previous field1 context into root context (by using rootContext())
// and then resolve second field context 
context = context.rootContext().fieldType(Root.getDeclaredField("field2"))

context.genericsMap() == ["A": Boolean, "T": String]
// outer class generics taken directly from field declaration, but note that A 
// is not counted as not reachable for inner class
context.ownerGenericsMap() == ["B": String, "C": String]
context.visibleGenericsMap() == ["A": Boolean, "T": String, "B": Integer, "C": Long]

// context.toString() (first Object is not a mistake! A was ignored in outer class):
// class Outer<Object, String, String>.Inner<Boolean, String>  resolved in context of Root    <-- current
```

Resulted inlying context can do everything root context can (context was just resolved with extra information)

```java
// navigate to method in inner class
context = context.method(Outer.Inner.getMethod("doSmth2"))

context.genericsMap() == ["A": Boolean, "T": String]
// owner generics always shown as is even when actually overridden (for consistency)
context.ownerGenericsMap() == ["B": String, "C": String]
context.methodGenericsMap() == ["B": Object]
// note that outer generic B is not visible (because of method generic)
context.visibleGenericsMap() == ["A": Boolean, "T": String, "B": Object, "C": String]

// context.toString():
// class Outer<Object, String, String>.Inner<Boolean, String>  resolved in context of Root
//   Object doSmth2()    <-- current
```


### Usage (theory)

NOTE: there are 2 APIs: context API (primary) used to support introspection (reflection analysis)
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

#### Partial hierarchy

You can limit hierarchy resolution depth (or exclude some interfaces) by providing ignored classes: 

```java
GenericsResolver.resolve(Root.class, Base.class, SomeInterface.class)
```

Here hierarchy resolution will stop on `Base` class (will not be included) and 
all appeared SomeInterfaces will be skipped (interfaces are also hierarchical so it could exclude sub hierarchy too).  

Option exists for very rare cases when some types breaks analysis (possible bug workaround).

WARNING: When ignored classes specified, resolved generics information is not cached(!) even if complete type resolution
was done before (descriptor always computed, but it's not expensive).

#### Context

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

#### Context class generics

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

#### Working with methods

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

#### Working with constructors

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

#### Working with fields

Context contains special shortcut methods for working with fields. For example,

```java
context.resolveFieldClass(field)
```

In essence, it's the same as: `context.resolveClass(field.getGenericType())`

It is just shorter and, if field does not belongs to current hierarchy, more concrete error will be thrown.
But it would be IllegalArgumentException instead of WrongGenericsContextException because
field case assumed to be simpler to track and more obvious to predict. 

#### Types resolution

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

#### Inlying context

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

NOTE: inlying context also inherits all ignored classes specified during root context 
creation (`GenericResolver.resolve(Root.class, [classes to ignore])`). 

If target type does not contains generics then type resolution will be cached
(because it is the same as direct type resolution).

##### Inlying inner classes

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

##### Inlying context for sub type

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

#### To string

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

### Low level api

Context, produced by `GenericsResolver` is just a convenient utility
to simplify usage. Internally it consists of a Map with resolved type generics and 
utilities calls, which may be used directly.

If known generics exists only as List, then it can be converted to map with:

```java
Map<String, Type> generics = GenericsUtils.createGenericsMap(Some.class, knownGenericsList);
```

#### Utilities

`TypeUtils` was already mentioned above - pure types operations (unknown generics ignored)
like `.isCompatible(Type, Type) == boolean`, `.getMoreSpecific(Type, Type)`, `.isAssignable(Type, Type)`

`TypeToStringUtils` - various to string helper methods  

`GenericsUtils` - generics manipulations (all `resolve*` methods from context) 
(requires known generics map to properly resolve types).

`GenericsResolutionUtils` - class analysis (mostly useful for root type resolution - hierarchy computation).
Creates generics maps, used for type resolutions. Special, and most useful case is direct class generics 
resolution (lower bounds): `GenericResolutionUtils.resolveRawGenetics(Class type) == Map<String, Type>`

`GenericsTrackingUtils` - resolution of root class's unknown generics by known middle class generics.
Used to compute more specific generics for root class before actual resolution (for inlying contexts).

`GenericInfoUtils` - `GenericsInfo` factory for all cases: direct class, sub type, and sub type with target class.
Essentially it's the same as GenericsResolver but without context wrapping (navigator) and without cache.

WARNING: some methods may not do what you expect! For example `TypeUtils.getOuter(Type)` is not the same as 
`Classs#getEnclosingClass()` (which returns outer class for static classes and interfaces too).
Another example is `ToStringUtils.toStringType()` which prints outer class only if provided type
is ParameterizedType with not null owner. I essence, api oriented to generic resolution cases and
all edge cases are described in javadoc.

#### Special maps

Special maps may be used for generics resolution:

* `IgnoreGenericsMap` - use to ignore unknown generics (instead of fail).
For example, `GenericsUtils.resolveClass(List<T>, new IgnoreGenericsMap()) == List.class`
* `PrintableGenericsMap` - special map for `TypeToStringUtils` to print unkown generics (instead of fail).
For example, `TypeToStringUtils.toStringType(List<T>, new PrintableGenericsMap()) == "List<T>"`  

#### Types walker

Special api for walking on two types side by side. 
Usages: compatibility check and more specific type detection.

```java
TypesWalker.walk(Type, Type, TypesVisitor);
```

`TypesVisitor` implementation receive either incompatibility signal 
(and processing stops after that) or types for comparison. Visitor could stop processing at any stage.

For example, for `List<String>` and `List<Integer>`:

```
next(List, List)
incompatibleHierarchy(String, Integer)
```

It will correctly balance types by building hierarchy, where 
appropriate:

`List<MyCallable<String>>` and `ArrayList<Callable<String>`:

```
next(List, ArrayList)
// resolve generic for List on the right
next(MyCallable, Callable)
// now compute callable generic on the left
next(String, String)
```

##### Types rules

*Java wildcard rules are not strictly followed* during type compatibility checks, because
many rules are useless at runtime.  

Object always assumed as not known type.

`List<?>` == `List` == `List<Object>` == `List<? super Object>` == `List<? extends Object>`

Object is compatible and assignable to everything. `Object` is assignable to `List<String>` and 
`List<String>` is assignable to `Object` (type not known - assuming compatibility). 

`<? extends Something>` is considered as just `Something`.

`<? super Something>` is compatible with any super type of `Something` but not with any
sub type (`SomethingExt extends Something`).

`Object` is assignable to `<? extends String>`, but later is more specific (contains more type information).

`<? super Number>` is assignable to `<? super Integer>`, but not opposite! 

Primitives are compared as wrapper types (e.g. `Integer` for `int`), but not primitive arrays!

Table below compares  different `TypeUtils` methods (implemented using walker api): 

| type 1 | type 2 | isAssignable | isCompatible | isMoreSpecific |
| ------ | ------ | ------------ | ------------ | -------------- |
| Object | List | + | + | - |
| String | Integer | - | - | - |
| List | Object | + | + | + |
| List | List | + | + | + |
| List | List\<String> | + | + | - |
| List\<String> | List\<Integer> | - | - | - |
| List\<String> | List | + | + | + |
| ArrayList | List<String> | + | + | + |
| List\<String> | ArrayList | - | + | - |
| List\<String> | ArrayList\<String> | - | + | - |
| List | List\<? super String> | + | + | - |
| List\<? super String> | List | + | + | + |
| List\<? super Number> | List\<? super Integer> | + | + | + |
| List\<String> | List\<? super String> | + | + | + |
| List\<? super String> | List\<String> | - | + | - |
| List[] | List\<? super String>[] | + | + | - |
| List\<? super String>[] | List[] | + | + | + |
| Integer[] | Object[] | + | + | + |
| Object[] | Integer[] | + | + | - |
| Some\<String, Object> | Some\<String, String> | + | + | - |
| Some\<String, String> | Some\<String, Object> | + | + | + |
| Some\<String, Long> | Some\<String, Boolean> | - | - | - |
| Integer | long | - | - | - |
| Integer | int | + | + | + |
| int | Number | + | + | + |
| Number | int | - | + | - |
| int | Comparable | + | + | + |
| int | Comparable\<Long> | - | - | - |
| int | long | - | - | - | 
| int[] | long[] | - | - | - |
| int[] | Object[] | - | - | - |
| int[] | Integer[] | - | - | - |

### Supplement

[reflection tutorial](http://www.javacodegeeks.com/2014/11/java-reflection-api-tutorial.html)

---
[![java lib generator](http://img.shields.io/badge/Powered%20by-%20Java%20lib%20generator-green.svg?style=flat-square)](https://github.com/xvik/generator-lib-java)

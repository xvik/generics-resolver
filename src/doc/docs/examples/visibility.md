# Generics visibility

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

## Type generics

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

## Constructor generics

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

## Method generics

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


## Outer class generics

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

!!! note 
    Tere was an assumption that as `Root` context contains `Outer` class (outer for `Inner`), then inner class was created inside it
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

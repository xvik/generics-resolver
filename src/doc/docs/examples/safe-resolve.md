# Safe resolution

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

Such detections are possible because java type variables always hold declaration information
and so library could compare current generics context type with generic declaration. This almost
avoids hard to track errors (which could appear due to resolution in wrong context).

## Possible cases

In some cases you can still get wrong results. For example, by forgetting to switch context.

In the above example both `Base` and `Base2` declares generic `T`. If you want to resolve
`Base` class generic, but, by accident, context type currently is at `Base2` (`.type(Base2.class)`)
then:

```java
context.generic("T") == String.class
```

You actually get generic of class `Base2`, because its currently selected, and it also has declared generic named 'T'
(library can't guess here your actual intention).   

!!! note ""
    Of course, if context class does not contain generic with requested name error would be thrown,
    but generics are named the same too often.   
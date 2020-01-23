# Type hierarchy

```java
class Base<T, K> {   
}

class Root extends Base<Integer, Long> {}
```

Get all types in class hierarchy (classes and interfaces):

```java
GenericsResolver.resove(Root).getGenericsInfo().getComposingTypes() == 
                                [Root.class, Base.class]
```

Print hierarchy:

```java
GenericsResolver.resove(Root).getGenericsInfo().toString();
```

```java
class Root
  extends Base<Integer, Long>
``` 

Hierarchy includes all classes and interfaces (even without declared generics).

## Interface specifics 

One interface may appear in multiple hierarchy "branches", but its generics will be 
either the same or compatible (otherwise java will simply not compile). Generics resolution
will select the most specific generics. For example:

```java
public interface Some<T, K> {}

public interface One extends Some<Long, Number> {}

public interface Two extends Some<Number, Integer> {}

public class Root implmenets One, Two {}
```

Generics of `Some` would be resolved (under `Root`) as `[Long, Integer]` (compilation from both declarations).
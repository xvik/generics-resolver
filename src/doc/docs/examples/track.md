# Generics tracking

Tracking is a reverse process to generics resolution: when you know some type's generics
and you want to calculate generics of some root type.

```java
class Base<T> {
    
}

class Root<K, P extends Comparable> extends Base<K[]> {}
```

Suppose we know `Base<String[]>` and we want to track `Root` class generics:

```java
GenericsTrackingUtils.track(Root, Base, ["T": String[]]) 
                        == ["K": String, "P": Comparable]
```

Here we don't have any connection from root generic "P" and base class, so its resolved
by raw declaration (as declared upper bound).

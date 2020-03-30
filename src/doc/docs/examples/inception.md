# Sub types analysis

Suppose you have hierarchical pojo (e.g. configuration class):

```java
class BaseConf<T> {
    SubConf<T> dbConf;
}

class Conf extends Base<String> {}
```

You want to analyze dbConf's type (`SubConf<T>` in context of `Conf`):

```java
// build context for root class
GenericsContext context = GenericsResolver.resove(Conf.class)
            // build sub context for field type
            .fieldType(BaseConf.class.getDeclaredField("dbConf"));

// working in sub context as in usual context
context.currentClass() == SubConf.class
context.generics() == [String.class]
...
```

The resulted context root is `SubConf<String>`.

Shown `context.fieldType(field)` is actually a shortcut for general mechanism of building 
sub contexts for any type ([inlying types](../guide/inlying.md)).

Inlying types used when target type may contain host type's generics.
For example, if we get field type directly with pure reflection `BaseConf.class.getDeclaredField("dbConf")`
returned type would contain variable 'T' and if we try to build new context directly, it would fail.

General sub-context creation from type looks like:

```java
GenericsContext context = GenericsResolver.resolve(Conf.class)
    .type(BaseConf.class)
    .inlyingType(BaseConf.class.getDeclaredField("dbConf"))
```

!!! tip
    Note that type change `.type(BaseConf.class)` is required now as initial 
    `Conf` type does not contain required generics. In case of field shortcut, 
    this context change was automatic (because `Field` contains declaration source)
    
Sub context may be build from any type: method parameter, constructor argument or even class's own generic:

```java
public class Middle<T, K extends Some<T>> {}
public class Root extends Middle<String, Some<String>> {}

GenericsContext context = GenericsResolver.resolve(Root.class).type(Middle.class);
// Some<String> 
GenericsContext inner = context.inlyingType(context.genericType("K"));
```
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
            .fieldType(Conf.class.getDeclaredField("dbConf"));

// working in sub context as in usual context
context.currentClass() == SubConf.class
context.generics() == [String.class]
...
```   
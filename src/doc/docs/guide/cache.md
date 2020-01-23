# Cache

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

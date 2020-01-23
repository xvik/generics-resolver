# Instance analysis

!!! warning ""
    This is very specific case when we use actual instances to get more type information.
    For example, when we have 
    ```java
    public class Some {
        Object something;
    }   
    ```
    We can't tell anything about something filed type (simply nothing declared). But if we have 
    instance of `Some`, we can look field *value* and improve known type information. 

Suppose you want to analyze current object **instance** structure.
For example, you have configuration object and
you want to compute its values as paths:

```java
class Conf {
    Base<String> sub;
}
```

```java
Conf conf = ...// instance
GenericsContext context = GenericsResolver.resolve(conf.getClass())

Map<String, PropertyPath> configPaths = new HashMap<>();
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
but we know generics only for its base class (`Base<String>`).
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

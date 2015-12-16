### 2.0.1 (2015-12-16)
* Fix dependent root generics resolution

### 2.0.0 (2015-06-27)
* Improve error reporting to show unknown generic name with analyzing type instead of NPE (simplifies usage errors understanding).
* Support method generics: new method context added (context.method(*)) to properly resolve generics including references to method generics
* (breaking change) method analysis methods (parameters and return type resolutions) are moved to method context. 
For example, before it was context.resolveParameters(method), now context.method(method).resolveParameters(). 

### 1.2.1 (2015-03-05)
* Improve duplicate interfaces support (thanks to [Adam Biczok](https://github.com/malary))

### 1.2.0 (2015-02-12)
* Root class generics now resolved (from generic bounds)
* Support broken hierarchies parsing (when root class generic passed or when target class did not set generics (as with root generics resolved from signature))

### 1.1.0 (2014-12-15)
* Add wildcards support
* Improve complex generics resolution
* Add access by generic name methods in GenericsContext
* Add ability to disable cache using property and method to clear current cache

### 1.0.0 (2014-11-19)
* Initial release
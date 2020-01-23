package ru.vyarus.java.generics.resolver;

/**
 * @author Vyacheslav Rusakov
 * @since 23.01.2020
 */
public class CommonsLangTestsSupport {
}

class AAClass<T> {

    public class BBClass<S> {
    }
}

class AAAClass extends AAClass<String> {
    public class BBBClass extends BBClass<String> {
    }
}

@SuppressWarnings("rawtypes")
//raw types, where used, are used purposely
// NOTE: allowed declaration for groovy 2.4 but not for 2.5
class AClass extends AAClass<String>.BBClass<Number> {

    AClass(final AAClass<String> enclosingInstance) {
        enclosingInstance.super();
    }

    public class BClass<T> {
    }

    public class CClass<T> extends BClass {
    }

    public class DClass<T> extends CClass<T> {
    }

    public class EClass<T> extends DClass {
    }

    public class FClass extends EClass<String> {
    }

    public class GClass<T extends BClass<? extends T> & AInterface<AInterface<? super T>>> {
    }

    public BClass<Number> bClass;

    public CClass<? extends String> cClass;

    public DClass<String> dClass;

    public EClass<String> eClass;

    public FClass fClass;

    public GClass gClass;

    public interface AInterface<T> {
    }
}

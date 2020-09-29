package ru.vyarus.java.generics.resolver.cases

import ru.vyarus.java.generics.resolver.util.TypeUtils
import spock.lang.Specification

import java.lang.annotation.Annotation
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy

/**
 * @author Vyacheslav Rusakov
 * @since 29.09.2020
 */
class AnnotationCaseTest extends Specification {

    def "Check annotation comparison"() {

        expect:
        TypeUtils.isCompatible(Annotation, Ann)
        TypeUtils.isAssignable(Ann, Annotation)
        TypeUtils.isMoreSpecific(Ann, Annotation)
    }
}

@Retention(RetentionPolicy.RUNTIME)
@interface Ann {
    String value()
}

package ru.vyarus.java.generics.resolver.support.wildcard

import ru.vyarus.java.generics.resolver.support.Model

/**
 * @author Vyacheslav Rusakov 
 * @since 15.12.2014
 */
interface WCRoot extends WCBase<? extends Model, ? super Model> {
}
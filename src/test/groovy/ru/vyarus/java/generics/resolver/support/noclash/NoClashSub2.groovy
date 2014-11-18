package ru.vyarus.java.generics.resolver.support.noclash

import java.util.concurrent.Callable

/**
 * @author Vyacheslav Rusakov 
 * @since 19.11.2014
 */
interface NoClashSub2 extends Runnable, Callable<Integer> {

}
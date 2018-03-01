package com.miruken.runtime

import org.junit.Test
import kotlin.reflect.KClass
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.allSupertypes
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.isSupertypeOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RuntimeHelpersTest {
    @Test fun `Can check KType for Unit`() {
        assertTrue(typeOf<Unit>().isUnit)
    }

    @Test fun `Non generic types are closed`() {
        assertFalse(typeOf<String>().isOpenGeneric)
    }

    @Test fun `Determines if generic type is closed`() {
        assertFalse(typeOf<List<String>>().isOpenGeneric)
    }

    @Test fun `Obtains component type of collection`() {
        val componentType = typeOf<List<Int>>().componentType
        assertEquals(typeOf<Int>(), componentType)
    }

    @Test fun `Obtains component type of array`() {
        val componentType = typeOf<Array<String>>().componentType
        assertEquals(typeOf<String>(), componentType)
    }

    @Test fun `gets all interfaces of type`() {
        val allInterfaces = typeOf<BazImpl>().allInterfaces
        assertEquals(7, allInterfaces.size)
        assertTrue(allInterfaces.containsAll(listOf(
                typeOf<Baz>(), typeOf<Foo>(), typeOf<Bar>(),
                typeOf<Boo>(), typeOf<List<String>>(),
                typeOf<Collection<String>>(), typeOf<Iterable<String>>()
        )))
    }

    @Test fun `gets all top-level interfaces of type`() {
        var topLevelInterfaces = typeOf<Baz>().allTopLevelInterfaces
        assertEquals(2, topLevelInterfaces.size)
        assertTrue(topLevelInterfaces.containsAll(listOf(
                typeOf<Bar>(), typeOf<Foo>()
        )))
        topLevelInterfaces = typeOf<BazImpl>().allTopLevelInterfaces
        assertEquals(3, topLevelInterfaces.size)
        assertTrue(topLevelInterfaces.containsAll(listOf(
                typeOf<Baz>(), typeOf<Boo>(), typeOf<List<String>>()
        )))
    }

    @Test fun `Determines if top-level interface`() {
        assertFalse(typeOf<Bar>().isTopLevelInterfaceOf<Bar>())
        assertTrue(typeOf<Bar>().isTopLevelInterfaceOf<BarImpl>())
        assertTrue(typeOf<Baz>().isTopLevelInterfaceOf<BazImpl>())
        assertTrue(typeOf<Boo>().isTopLevelInterfaceOf<BazImpl>())
        assertFalse(typeOf<Bar>().isTopLevelInterfaceOf<BazImpl>())
        assertFalse(typeOf<Foo>().isTopLevelInterfaceOf<BarImpl>())
        assertFalse(typeOf<Foo>().isTopLevelInterfaceOf<BazImpl>())
        var x = typeOf<BamImpl<Int>>().allTopLevelInterfaces
        //assertTrue(typeOf<Bam<Int>>().isTopLevelInterfaceOf<BamImpl<Int>>())
    }

    interface Foo
    interface Bar
    interface Boo : Bar
    interface Baz : Foo, Bar
    interface Bam<T> : Foo
    open class BarImpl : Bar
    abstract class BazImpl : Baz, Boo, List<String>
    abstract class BamImpl<T> : BazImpl(), Foo, Bam<T>
}

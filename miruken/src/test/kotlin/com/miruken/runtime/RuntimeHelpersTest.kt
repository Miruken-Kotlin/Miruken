package com.miruken.runtime

import com.miruken.kTypeOf
import com.miruken.typeOf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class RuntimeHelpersTest {
    @Test fun `Can check KType for Unit`() {
        assertTrue(kTypeOf<Unit>().isUnit)
    }

    @Test fun `Non generic types are closed`() {
        assertFalse(kTypeOf<String>().isOpenGeneric)
    }

    @Test fun `Determines if generic type is closed`() {
        assertFalse(kTypeOf<List<String>>().isOpenGeneric)
    }

    @Test fun `Obtains component type of collection`() {
        val componentType = kTypeOf<List<Int>>().componentType
        assertEquals(kTypeOf<Int>(), componentType)
    }

    @Test fun `Obtains component type of array`() {
        val componentType = kTypeOf<Array<String>>().componentType
        assertEquals(kTypeOf<String>(), componentType)
    }

    @Test fun `Converts untyped collection into typed array`() {
        val list: List<*> = listOf(BarImpl(), BarImpl())
        val array = list.toTypedArray(BarImpl::class) as Array<*>
        assertEquals(BarImpl::class.java, array::class.java.componentType)
        assertEquals(2, array.size)
        assertSame(list[0], array[0])
        assertSame(list[1], array[1])
    }

    @Test fun `Converts untyped primitive collection into typed array`() {
        val list: List<*> = listOf(1, 2)
        val array = list.toTypedArray(Int::class.java) as IntArray
        assertEquals(Int::class.java, array::class.java.componentType)
        assertEquals(2, array.size)
        assertSame(list[0], array[0])
    }

    @Test fun `gets all interfaces of type`() {
        val allInterfaces = kTypeOf<BazImpl>().allInterfaces
        assertEquals(7, allInterfaces.size)
        assertTrue(allInterfaces.containsAll(listOf(
                kTypeOf<Baz>(), kTypeOf<Foo>(), kTypeOf<Bar>(),
                kTypeOf<Boo>(), kTypeOf<List<String>>(),
                kTypeOf<Collection<String>>(), kTypeOf<Iterable<String>>()
        )))
    }

    @Test fun `gets all top-level interfaces of type`() {
        var topLevelInterfaces = kTypeOf<Baz>().allTopLevelInterfaces
        assertEquals(2, topLevelInterfaces.size)
        assertTrue(topLevelInterfaces.containsAll(listOf(
                kTypeOf<Bar>(), kTypeOf<Foo>()
        )))
        topLevelInterfaces = kTypeOf<BazImpl>().allTopLevelInterfaces
        assertEquals(3, topLevelInterfaces.size)
        assertTrue(topLevelInterfaces.containsAll(listOf(
                kTypeOf<Baz>(), kTypeOf<Boo>(), kTypeOf<List<String>>()
        )))
    }

    @Test fun `Determines if top-level interface`() {
        assertFalse(kTypeOf<Bar>().isTopLevelInterfaceOf<Bar>())
        assertTrue(kTypeOf<Bar>().isTopLevelInterfaceOf<BarImpl>())
        assertTrue(kTypeOf<Baz>().isTopLevelInterfaceOf<BazImpl>())
        assertTrue(kTypeOf<Boo>().isTopLevelInterfaceOf<BazImpl>())
        assertFalse(kTypeOf<Bar>().isTopLevelInterfaceOf<BazImpl>())
        assertFalse(kTypeOf<Foo>().isTopLevelInterfaceOf<BarImpl>())
        assertFalse(kTypeOf<Foo>().isTopLevelInterfaceOf<BazImpl>())
        //var x = typeOf<BamImpl<Int>>().allTopLevelInterfaces
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

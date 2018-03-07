package com.miruken.runtime

import com.miruken.UNIT_TYPE
import com.miruken.typeOf
import org.junit.Test
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Suppress("Classname")
class typeOfTest {
    class Foo
    class Bar<T>

    @Test fun `Can obtain KType of T`() {
        assertEquals(typeOf<Set<String>>(), typeOf<Set<String>>())
        assertNotEquals(typeOf<Set<String>>(), typeOf<Set<Int>>())
        assertEquals(typeOf<List<*>>(), typeOf<List<Any>>())
    }

    @Test fun `Can obtain component KType of Unit`() {
        val unitType = typeOf<Unit>()
        assertTrue(unitType.isSubtypeOf(UNIT_TYPE))
    }

    @Test fun `Can obtain component KType of Any`() {
        val anyType = typeOf<Any>()
        assertTrue(anyType.isSubtypeOf(ANY_TYPE))
    }

    @Test fun `Can obtain component KType of List`() {
        val listType = typeOf<List<String>>()
        assertTrue(listType.isSubtypeOf(typeOf<List<*>>()))
        val componentType = listType.arguments.single().type
        assertEquals(typeOf<String>(), componentType)
    }

    @Test fun `Can obtain KType of Function`() {
        val listType = typeOf<(String) -> Int>()
        assertEquals(Function1::class, listType.classifier)
        assertEquals(typeOf<String>(), listType.arguments[0].type)
        assertEquals(typeOf<Int>(), listType.arguments[1].type)
    }

    @Test fun `Can check KType assignability`() {
        assertTrue(isCompatibleWith(
                typeOf<List<*>>(), typeOf<List<String>>()))
        assertFalse(isCompatibleWith(
                typeOf<List<Foo>>(), typeOf<List<*>>()))
        assertFalse(isCompatibleWith(
                typeOf<List<Int>>(), typeOf<List<String>>()))
    }

    @Test fun `Can check KClass assignability`() {
        assertTrue(isCompatibleWith(Foo::class, Foo()))
        assertTrue(isCompatibleWith(Foo::class, Foo()::class))
        assertTrue(isCompatibleWith(Foo::class, Foo().javaClass))
        assertFalse(isCompatibleWith(Foo(), Foo::class))
        assertFalse(isCompatibleWith(String::class, Foo()::class))
        assertTrue(isCompatibleWith(Bar::class, Bar<Int>()::class))
    }

    @Test fun `Can check mixed assignability`() {
        assertTrue(isCompatibleWith(typeOf<Foo>(), Foo()))
        assertTrue(isCompatibleWith(Foo::class, typeOf<Foo>()))
        assertTrue(isCompatibleWith(typeOf<Foo>(), Foo::class))
        assertFalse(isCompatibleWith(Foo(), typeOf<Foo>()))
        assertFalse(isCompatibleWith(typeOf<Bar<String>>(), Bar<String>()))
    }

    @Test fun `Can check primitive assignability`() {
        assertTrue(isCompatibleWith(typeOf<Int>(), 2))
        assertTrue(isCompatibleWith(typeOf<Int>(), Int::class))
        assertTrue(isCompatibleWith(Int::class, 2))
        assertTrue(isCompatibleWith(typeOf<Int>(), 2::class.java))
    }

    @Test fun `Can determine KType covariance`() {
        assertTrue(typeOf<MutableList<Int>>()
                .isSubtypeOf(typeOf<MutableList<Any>>()))
        assertFalse(typeOf<MutableList<Any>>()
                .isSubtypeOf(typeOf<MutableList<Int>>()))
    }

    @Test fun `Can determine KType wildcard covariance`() {
        assertTrue(typeOf<MutableList<Int>>()
                .isSubtypeOf(typeOf<MutableList<*>>()))
        assertFalse(typeOf<MutableList<*>>()
                .isSubtypeOf(typeOf<MutableList<Int>>()))
    }

    @Test fun `Can determine KType contravariance`() {
        assertTrue(typeOf<Comparable<Any>>()
                .isSubtypeOf(typeOf<Comparable<String>>()))
        assertFalse(typeOf<Comparable<String>>()
                .isSubtypeOf(typeOf<Comparable<Any>>()))
    }

    @Test fun `Can determine KType wildcard contravariance`() {
        assertFalse(typeOf<Comparable<*>>()
                .isSubtypeOf(typeOf<Comparable<String>>()))
        assertTrue(typeOf<Comparable<String>>()
                .isSubtypeOf(typeOf<Comparable<*>>()))
    }

    @Test fun `Can determine KType in generic function`() {
        assertEquals(typeOf<String>(), something<String>())
        assertEquals(typeOf<Foo>(), something<Foo>())
        assertEquals(typeOf<Bar<Int>>(), something<Bar<Int>>())
    }

    private inline fun <reified T: Any> something(): KType = typeOf<T>()
}

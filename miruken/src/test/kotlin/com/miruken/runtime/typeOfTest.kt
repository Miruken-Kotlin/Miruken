package com.miruken.runtime

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

    @Test fun `Can obtain KType from reified T`() {
        assertEquals(typeOf<Set<String>>(), typeOf<Set<String>>())
        assertNotEquals(typeOf<Set<String>>(), typeOf<Set<Int>>())
        assertEquals(typeOf<List<*>>(), typeOf<List<Any>>())
    }

    @Test fun `Can obtain component KType of List`() {
        val listType = typeOf<List<String>>()
        assertTrue(listType.isSubtypeOf(typeOf<List<*>>()))
        val componentType = listType.arguments.single().type
        assertEquals(typeOf<String>(), componentType)
    }

    @Test fun `Can obtain KType of Function`() {
        val listType = typeOf<(String)->Int>()
        assertEquals(Function1::class, listType.classifier)
        assertEquals(typeOf<String>(), listType.arguments[0].type)
        assertEquals(typeOf<Int>(), listType.arguments[1].type)
    }

    @Test fun `Can check KType assignability`() {
        assertTrue(isAssignableTo(
                typeOf<List<*>>(), typeOf<List<String>>()))
        assertFalse(isAssignableTo(
                typeOf<List<Foo>>(), typeOf<List<*>>()))
        assertFalse(isAssignableTo(
                typeOf<List<Int>>(), typeOf<List<String>>()))
    }

    @Test fun `Can check KClass assignability`() {
        assertTrue(isAssignableTo(Foo::class, Foo()))
        assertTrue(isAssignableTo(Foo::class, Foo()::class))
        assertTrue(isAssignableTo(Foo::class, Foo().javaClass))
        assertFalse(isAssignableTo(Foo(), Foo::class))
        assertFalse(isAssignableTo(String::class, Foo()::class))
        assertTrue(isAssignableTo(Bar::class, Bar<Int>()::class))
    }

    @Test fun `Can check mixed assignability`() {
        assertTrue(isAssignableTo(typeOf<Foo>(), Foo()))
        assertTrue(isAssignableTo(Foo::class, typeOf<Foo>()))
        assertTrue(isAssignableTo(typeOf<Foo>(), Foo::class))
        assertFalse(isAssignableTo(Foo(), typeOf<Foo>()))
        assertFalse(isAssignableTo(typeOf<Bar<String>>(), Bar<String>()))
    }

    @Test fun `Can check primitive assignability`() {
        assertTrue(isAssignableTo(typeOf<Int>(), 2))
        assertTrue(isAssignableTo(typeOf<Int>(), Int::class))
        assertTrue(isAssignableTo(Int::class, 2))
        assertTrue(isAssignableTo(typeOf<Int>(), 2::class.java))
    }

    @Test fun `Can check assignability to null`() {
        assertFalse(isAssignableTo(Foo::class, null))
        assertFalse(isAssignableTo(String::class, null))
        assertFalse(isAssignableTo(Bar::class, null))
        assertFalse(isAssignableTo(typeOf<Foo>(), null))
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

    private inline fun <reified T: Any> something() : KType = typeOf<T>()
}

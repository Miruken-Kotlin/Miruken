package com.miruken.runtime

import org.junit.Test
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

@Suppress("Classname")
class getKTypeTest {
    class Foo
    class Bar<T>

    @Test fun `Can obtain KType from reified T`() {
        assertEquals(getKType<Set<String>>(), getKType<Set<String>>())
        assertNotEquals(getKType<Set<String>>(), getKType<Set<Int>>())
        assertEquals(getKType<List<*>>(), getKType<List<Any>>())
    }

    @Test fun `Can obtain component KType of List`() {
        val listType = getKType<List<String>>()
        assertTrue { listType.isSubtypeOf(getKType<List<*>>()) }
        val componentType = listType.arguments.single().type
        assertEquals(getKType<String>(), componentType)
    }

    @Test fun `Can obtain KType of Function`() {
        val listType = getKType<(String)->Int>()
        assertEquals(Function1::class, listType.classifier)
        assertEquals(getKType<String>(), listType.arguments[0].type)
        assertEquals(getKType<Int>(), listType.arguments[1].type)
    }

    @Test fun `Can check KType assignability`() {
        assertTrue { isAssignableTo(
                getKType<List<*>>(), getKType<List<String>>()) }
        assertFalse { isAssignableTo(
                getKType<List<Foo>>(), getKType<List<*>>()) }
        assertFalse { isAssignableTo(
                getKType<List<Int>>(), getKType<List<String>>()) }
    }

    @Test fun `Can check KClass assignability`() {
        assertTrue  { isAssignableTo(Foo::class, Foo()) }
        assertTrue  { isAssignableTo(Foo::class, Foo()::class) }
        assertTrue  { isAssignableTo(Foo::class, Foo().javaClass) }
        assertFalse { isAssignableTo(Foo(), Foo::class) }
        assertFalse { isAssignableTo(String::class, Foo()::class) }
        assertFalse { isAssignableTo(Bar::class, Bar<Int>()::class) }
    }

    @Test fun `Can check mixed assignability`() {
        assertTrue  { isAssignableTo(getKType<Foo>(), Foo()) }
        assertTrue  { isAssignableTo(Foo::class, getKType<Foo>()) }
        assertTrue  { isAssignableTo(getKType<Foo>(), Foo::class) }
        assertFalse { isAssignableTo(Foo(), getKType<Foo>()) }
        assertFalse { isAssignableTo(getKType<Bar<String>>(), Bar<String>()) }
    }

    @Test fun `Can check primitive assignability`() {
        assertTrue  { isAssignableTo(getKType<Int>(), 2) }
        assertTrue  { isAssignableTo(getKType<Int>(), Int::class) }
        assertTrue  { isAssignableTo(Int::class, 2) }
        assertTrue  { isAssignableTo(getKType<Int>(), 2::class.java) }
    }

    @Test fun `Can check assignability to null`() {
        assertFalse { isAssignableTo(Foo::class, null) }
        assertFalse { isAssignableTo(String::class, null) }
        assertFalse { isAssignableTo(Bar::class, null) }
        assertFalse { isAssignableTo(getKType<Foo>(), null) }
    }

    @Test fun `Can determine KType covariance`() {
        assertTrue { getKType<MutableList<Int>>()
                .isSubtypeOf(getKType<MutableList<Any>>()) }
        assertFalse { getKType<MutableList<Any>>()
                .isSubtypeOf(getKType<MutableList<Int>>()) }
    }

    @Test fun `Can determine KType wildcard covariance`() {
        assertTrue { getKType<MutableList<Int>>()
                .isSubtypeOf(getKType<MutableList<*>>()) }
        assertFalse { getKType<MutableList<*>>()
                .isSubtypeOf(getKType<MutableList<Int>>()) }
    }

    @Test fun `Can determine KType contravariance`() {
        assertTrue { getKType<Comparable<Any>>()
                .isSubtypeOf(getKType<Comparable<String>>()) }
        assertFalse { getKType<Comparable<String>>()
                .isSubtypeOf(getKType<Comparable<Any>>()) }
    }

    @Test fun `Can determine KType wildcard contravariance`() {
        assertFalse { getKType<Comparable<*>>()
                .isSubtypeOf(getKType<Comparable<String>>()) }
        assertTrue { getKType<Comparable<String>>()
                .isSubtypeOf(getKType<Comparable<*>>()) }
    }

    @Test fun `Can determine KType in generic function`() {
        assertEquals(getKType<String>(), something<String>())
        assertEquals(getKType<Foo>(), something<Foo>())
        assertEquals(getKType<Bar<Int>>(), something<Bar<Int>>())
    }

    private inline fun <reified T: Any> something() : KType = getKType<T>()
}

package com.miruken.runtime

import org.junit.Test
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class RuntimeHelpersTest {
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

    @Test fun `Can check KType assignability`() {
        assertTrue { isAssignableKeys(
                getKType<List<*>>(), getKType<List<String>>()) }

        assertFalse { isAssignableKeys(
                getKType<List<Foo>>(), getKType<List<*>>()) }

        assertFalse { isAssignableKeys(
                getKType<List<Int>>(), getKType<List<String>>()) }
    }

    @Test fun `Can check KClass assignability`() {
        assertTrue  { isAssignableKeys(Foo::class, Foo()::class) }
        assertFalse { isAssignableKeys(String::class, Foo()::class) }
        assertFalse { isAssignableKeys(Bar::class, Bar<Int>()::class) }
    }

    @Test fun `Can check mixed assignability`() {
        assertTrue  { isAssignableKeys(Foo::class, getKType<Foo>()) }
        assertTrue  { isAssignableKeys(getKType<Foo>(), Foo::class) }
        assertFalse { isAssignableKeys(getKType<Bar<String>>(), Bar<String>()) }
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
}

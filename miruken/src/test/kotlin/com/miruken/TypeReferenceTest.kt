package com.miruken

import com.miruken.runtime.isCompatibleWith
import org.junit.Test
import kotlin.reflect.KType
import kotlin.reflect.full.isSubtypeOf

class TypeReferenceTest {
    class Foo
    class Bar<T>

    @Test
    fun `Can obtain KType of T`() {
        kotlin.test.assertEquals(typeOf<Set<String>>(), typeOf<Set<String>>())
        kotlin.test.assertNotEquals(typeOf<Set<String>>(), typeOf<Set<Int>>())
        kotlin.test.assertEquals(typeOf<List<*>>(), typeOf<List<Any>>())
    }

    @Test
    fun `Can obtain component KType of Unit`() {
        val unitType = typeOf<Unit>()
        kotlin.test.assertTrue(unitType.isSubtypeOf(TypeReference.UNIT_TYPE))
    }

    @Test
    fun `Can obtain component KType of Any`() {
        val anyType = typeOf<Any>()
        kotlin.test.assertTrue(anyType.isSubtypeOf(TypeReference.ANY_TYPE))
    }

    @Test
    fun `Can obtain component KType of List`() {
        val listType = typeOf<List<String>>()
        kotlin.test.assertTrue(listType.isSubtypeOf(typeOf<List<*>>()))
        val componentType = listType.arguments.single().type
        kotlin.test.assertEquals(typeOf<String>(), componentType)
    }

    @Test
    fun `Can obtain KType of Function`() {
        val listType = typeOf<(String) -> Int>()
        kotlin.test.assertEquals(Function1::class, listType.classifier)
        kotlin.test.assertEquals(typeOf<String>(), listType.arguments[0].type)
        kotlin.test.assertEquals(typeOf<Int>(), listType.arguments[1].type)
    }

    @Test
    fun `Can check KType assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(
                typeOf<List<*>>(), typeOf<List<String>>()))
        kotlin.test.assertFalse(isCompatibleWith(
                typeOf<List<Foo>>(), typeOf<List<*>>()))
        kotlin.test.assertFalse(isCompatibleWith(
                typeOf<List<Int>>(), typeOf<List<String>>()))
    }

    @Test
    fun `Can check KClass assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, Foo()))
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, Foo()::class))
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, Foo().javaClass))
        kotlin.test.assertFalse(isCompatibleWith(Foo(), Foo::class))
        kotlin.test.assertFalse(isCompatibleWith(String::class, Foo()::class))
        kotlin.test.assertTrue(isCompatibleWith(Bar::class, Bar<Int>()::class))
    }

    @Test
    fun `Can check mixed assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(typeOf<Foo>(), Foo()))
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, typeOf<Foo>()))
        kotlin.test.assertTrue(isCompatibleWith(typeOf<Foo>(), Foo::class))
        kotlin.test.assertFalse(isCompatibleWith(Foo(), typeOf<Foo>()))
        kotlin.test.assertFalse(isCompatibleWith(typeOf<Bar<String>>(), Bar<String>()))
    }

    @Test
    fun `Can check primitive assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(typeOf<Int>(), 2))
        kotlin.test.assertTrue(isCompatibleWith(typeOf<Int>(), Int::class))
        kotlin.test.assertTrue(isCompatibleWith(Int::class, 2))
        kotlin.test.assertTrue(isCompatibleWith(typeOf<Int>(), 2::class.java))
    }

    @Test
    fun `Can determine KType covariance`() {
        kotlin.test.assertTrue(typeOf<MutableList<Int>>()
                .isSubtypeOf(typeOf<MutableList<Any>>()))
        kotlin.test.assertFalse(typeOf<MutableList<Any>>()
                .isSubtypeOf(typeOf<MutableList<Int>>()))
    }

    @Test
    fun `Can determine KType wildcard covariance`() {
        kotlin.test.assertTrue(typeOf<MutableList<Int>>()
                .isSubtypeOf(typeOf<MutableList<*>>()))
        kotlin.test.assertFalse(typeOf<MutableList<*>>()
                .isSubtypeOf(typeOf<MutableList<Int>>()))
    }

    @Test
    fun `Can determine KType contravariance`() {
        kotlin.test.assertTrue(typeOf<Comparable<Any>>()
                .isSubtypeOf(typeOf<Comparable<String>>()))
        kotlin.test.assertFalse(typeOf<Comparable<String>>()
                .isSubtypeOf(typeOf<Comparable<Any>>()))
    }

    @Test
    fun `Can determine KType wildcard contravariance`() {
        kotlin.test.assertFalse(typeOf<Comparable<*>>()
                .isSubtypeOf(typeOf<Comparable<String>>()))
        kotlin.test.assertTrue(typeOf<Comparable<String>>()
                .isSubtypeOf(typeOf<Comparable<*>>()))
    }

    @Test
    fun `Can determine KType in generic function`() {
        kotlin.test.assertEquals(typeOf<String>(), something<String>())
        kotlin.test.assertEquals(typeOf<Foo>(), something<Foo>())
        kotlin.test.assertEquals(typeOf<Bar<Int>>(), something<Bar<Int>>())
    }

    private inline fun <reified T: Any> something(): KType = typeOf<T>()
}
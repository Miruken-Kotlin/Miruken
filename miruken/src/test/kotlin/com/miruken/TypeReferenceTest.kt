package com.miruken

import com.miruken.runtime.isCompatibleWith
import org.junit.Test
import kotlin.reflect.full.isSubtypeOf

class TypeReferenceTest {
    class Foo
    class Bar<T>

    @Test fun `Can obtain KType of T`() {
        kotlin.test.assertEquals(typeOf<Set<String>>(), typeOf<Set<String>>())
        kotlin.test.assertNotEquals(typeOf<Set<String>>().type, typeOf<Set<Int>>().type)
        kotlin.test.assertEquals(typeOf<List<*>>().type, typeOf<List<Any>>().type)
    }

    @Test fun `Can obtain component KType of Unit`() {
        val unitType = typeOf<Unit>()
        kotlin.test.assertTrue(unitType.kotlinType.isSubtypeOf(TypeReference.UNIT_TYPE))
    }

    @Test fun `Can obtain component KType of Any`() {
        val anyType = typeOf<Any>()
        kotlin.test.assertTrue(anyType.kotlinType.isSubtypeOf(TypeReference.ANY_TYPE))
    }

    @Test fun `Can obtain component KType of List`() {
        val listType = typeOf<List<String>>()
        kotlin.test.assertTrue(listType.kotlinType
                .isSubtypeOf(typeOf<List<*>>().kotlinType))
        val componentType = listType.kotlinType.arguments.single().type
        kotlin.test.assertEquals(kTypeOf<String>(), componentType)
    }

    @Test fun `Can obtain KType of Function`() {
        val listType = kTypeOf<(String) -> Int>()
        kotlin.test.assertEquals(Function1::class, listType.classifier)
        kotlin.test.assertEquals(kTypeOf<String>(), listType.arguments[0].type)
        kotlin.test.assertEquals(kTypeOf<Int>(), listType.arguments[1].type)
    }

    @Test fun `Can check KType assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(
                kTypeOf<List<*>>(), kTypeOf<List<String>>()))
        kotlin.test.assertFalse(isCompatibleWith(
                kTypeOf<List<Foo>>(), kTypeOf<List<*>>()))
        kotlin.test.assertFalse(isCompatibleWith(
                kTypeOf<List<Int>>(), kTypeOf<List<String>>()))
    }

    @Test fun `Can check KClass assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, Foo()))
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, Foo()::class))
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, Foo().javaClass))
        kotlin.test.assertFalse(isCompatibleWith(Foo(), Foo::class))
        kotlin.test.assertFalse(isCompatibleWith(String::class, Foo()::class))
        kotlin.test.assertTrue(isCompatibleWith(Bar::class, Bar<Int>()::class))
    }

    @Test fun `Can check mixed assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(kTypeOf<Foo>(), Foo()))
        kotlin.test.assertTrue(isCompatibleWith(Foo::class, kTypeOf<Foo>()))
        kotlin.test.assertTrue(isCompatibleWith(kTypeOf<Foo>(), Foo::class))
        kotlin.test.assertFalse(isCompatibleWith(Foo(), kTypeOf<Foo>()))
        kotlin.test.assertFalse(isCompatibleWith(kTypeOf<Bar<String>>(), Bar<String>()))
    }

    @Test fun `Can check primitive assignability`() {
        kotlin.test.assertTrue(isCompatibleWith(kTypeOf<Int>(), 2))
        kotlin.test.assertTrue(isCompatibleWith(kTypeOf<Int>(), Int::class))
        kotlin.test.assertTrue(isCompatibleWith(Int::class, 2))
        kotlin.test.assertTrue(isCompatibleWith(kTypeOf<Int>(), 2::class.java))
    }

    @Test fun `Can determine KType covariance`() {
        kotlin.test.assertTrue(typeOf<MutableList<Int>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<Any>>()))
        kotlin.test.assertFalse(typeOf<MutableList<Any>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<Int>>()))
    }

    @Test fun `Can determine KType wildcard covariance`() {
        kotlin.test.assertTrue(typeOf<MutableList<Int>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<*>>()))
        kotlin.test.assertFalse(typeOf<MutableList<*>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<Int>>()))
    }

    @Test fun `Can determine KType contravariance`() {
        kotlin.test.assertTrue(kTypeOf<Comparable<Any>>()
                .isSubtypeOf(kTypeOf<Comparable<String>>()))
        kotlin.test.assertFalse(kTypeOf<Comparable<String>>()
                .isSubtypeOf(kTypeOf<Comparable<Any>>()))
    }

    @Test fun `Can determine KType wildcard contravariance`() {
        kotlin.test.assertFalse(kTypeOf<Comparable<*>>()
                .isSubtypeOf(kTypeOf<Comparable<String>>()))
        kotlin.test.assertTrue(kTypeOf<Comparable<String>>()
                .isSubtypeOf(kTypeOf<Comparable<*>>()))
    }
}
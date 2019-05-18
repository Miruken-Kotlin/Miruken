package com.miruken

import com.miruken.runtime.isCompatibleWith
import org.junit.Test
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class TypeReferenceTest {
    open class Foo
    class SpecialFoo : Foo()
    open class Bar<T>
    class SpecialBar<T> : Bar<T>()

    @Test fun `Can obtain KType of T`() {
        assertEquals(typeOf<Set<String>>(), typeOf<Set<String>>())
        assertNotEquals(typeOf<Set<String>>().type, typeOf<Set<Int>>().type)
        assertEquals(typeOf<List<*>>().type, typeOf<List<Any>>().type)
    }

    @Test fun `Can obtain component KType of Unit`() {
        val unitType = typeOf<Unit>()
        assertTrue(unitType.kotlinType.isSubtypeOf(TypeReference.UNIT_TYPE))
    }

    @Test fun `Can obtain component KType of Any`() {
        val anyType = typeOf<Any>()
        assertTrue(anyType.kotlinType.isSubtypeOf(TypeReference.ANY_TYPE))
    }

    @Test fun `Can obtain component KType of List`() {
        val listType = typeOf<List<String>>()
        assertTrue(listType.kotlinType
                .isSubtypeOf(typeOf<List<*>>().kotlinType))
        val componentType = listType.kotlinType.arguments.single().type
        assertEquals(kTypeOf<String>(), componentType)
    }

    @Test fun `Can obtain KType of Function`() {
        val listType = kTypeOf<(String) -> Int>()
        assertEquals(Function1::class, listType.classifier)
        assertEquals(kTypeOf<String>(), listType.arguments[0].type)
        assertEquals(kTypeOf<Int>(), listType.arguments[1].type)
    }

    @Test fun `Can check KType assignability`() {
        assertTrue(isCompatibleWith(
                kTypeOf<List<*>>(), kTypeOf<List<String>>()))
        assertFalse(isCompatibleWith(
                kTypeOf<List<Foo>>(), kTypeOf<List<*>>()))
        assertFalse(isCompatibleWith(
                kTypeOf<List<Int>>(), kTypeOf<List<String>>()))
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
        assertTrue(isCompatibleWith(kTypeOf<Foo>(), Foo()))
        assertTrue(isCompatibleWith(Foo::class, kTypeOf<Foo>()))
        assertTrue(isCompatibleWith(kTypeOf<Foo>(), Foo::class))
        assertFalse(isCompatibleWith(Foo(), kTypeOf<Foo>()))
        assertFalse(isCompatibleWith(kTypeOf<Bar<String>>(), Bar<String>()))
    }

    @Test fun `Can check primitive assignability`() {
        assertTrue(isCompatibleWith(kTypeOf<Int>(), 2))
        assertTrue(isCompatibleWith(kTypeOf<Int>(), Int::class))
        assertTrue(isCompatibleWith(Int::class, 2))
        assertTrue(isCompatibleWith(kTypeOf<Int>(), 2::class.java))
    }

    @Test fun `Can determine KType covariance`() {
        assertTrue(typeOf<MutableList<Int>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<Any>>()))
        assertFalse(typeOf<MutableList<Any>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<Int>>()))
    }

    @Test fun `Can determine KType wildcard covariance`() {
        assertTrue(typeOf<MutableList<Int>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<*>>()))
        assertFalse(typeOf<MutableList<*>>()
                .kotlinType.isSubtypeOf(kTypeOf<MutableList<Int>>()))
    }

    @Test fun `Can determine KType contravariance`() {
        assertTrue(kTypeOf<Comparable<Any>>()
                .isSubtypeOf(kTypeOf<Comparable<String>>()))
        assertFalse(kTypeOf<Comparable<String>>()
                .isSubtypeOf(kTypeOf<Comparable<Any>>()))
    }

    @Test fun `Can determine KType wildcard contravariance`() {
        assertFalse(kTypeOf<Comparable<*>>()
                .isSubtypeOf(kTypeOf<Comparable<String>>()))
        assertTrue(kTypeOf<Comparable<String>>()
                .isSubtypeOf(kTypeOf<Comparable<*>>()))
    }

    @Test fun `Can obtain specific type of instance`() {
        val type = typeOf<Foo>().getMostSpecificType(SpecialFoo())
        assertEquals(typeOf<SpecialFoo>().kotlinType, type?.kotlinType)
        assertEquals(SpecialFoo::class.java, type?.type)
    }

    @Test fun `Cannot obtain specific type of generic instance`() {
        val type = typeOf<Bar<String>>().getMostSpecificType(SpecialBar<String>())
        assertEquals(typeOf<Bar<String>>().kotlinType, type?.kotlinType)
    }
}
package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.getKType
import org.junit.*
import org.junit.Test
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.starProjectedType
import kotlin.test.*

class InquiryTest {
    class Foo
    class Bar<out T>

    @Test fun `Creates Inquiry of String`() {
        val inquiry = Inquiry("Service")
        assertFalse(inquiry.many)
        assertFalse(inquiry.wantsAsync)
        assertEquals("Service", inquiry.key)
        assertEquals(getKType<Any>(), inquiry.resultType)
        assertSame(ProvidesPolicy, inquiry.policy)
    }

    @Test fun `Creates Inquiry of KClass`() {
        val inquiry = Inquiry(Foo::class)
        assertEquals(Foo::class, inquiry.key)
        assertEquals(getKType<Foo>(), inquiry.resultType)
    }

    @Test fun `Creates Inquiry of generic KClass`() {
        val inquiry = Inquiry(Bar::class)
        assertEquals(Bar::class, inquiry.key)
        assertTrue(getKType<Bar<*>>().isSubtypeOf(inquiry.resultType!!))
    }

    @Test fun `Creates Inquiry of KType`() {
        val inquiry = Inquiry(getKType<Bar<String>>())
        assertEquals(getKType<Bar<String>>(), inquiry.key)
        assertEquals(getKType<Bar<String>>(), inquiry.resultType)
    }

    @Test fun `Creates many Inquiry of KType`() {
        val inquiry = Inquiry(getKType<Bar<String>>(), true)
        assertEquals(getKType<Bar<String>>(), inquiry.key)
        assertEquals(getKType<List<Bar<String>>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of String`() {
        val inquiry = Inquiry("Service").apply { wantsAsync = true }
        assertTrue(inquiry.wantsAsync)
        assertEquals("Service", inquiry.key)
        assertEquals(getKType<Promise<*>>(), inquiry.resultType)
        assertSame(ProvidesPolicy, inquiry.policy)
    }

    @Test fun `Creates async Inquiry of KClass`() {
        val inquiry = Inquiry(Foo::class).apply {
            wantsAsync = true
        }
        assertEquals(Foo::class, inquiry.key)
        assertEquals(getKType<Promise<Foo>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of generic KClass`() {
        val inquiry = Inquiry(Bar::class).apply { wantsAsync = true }
        assertEquals(Bar::class, inquiry.key)
        assertTrue(getKType<Promise<Bar<*>>>().isSubtypeOf(inquiry.resultType!!))
    }

    @Test fun `Creates async many Inquiry of KType`() {
        val inquiry = Inquiry(getKType<Bar<String>>(), true).apply {
            wantsAsync = true
        }
        assertEquals(getKType<Bar<String>>(), inquiry.key)
        assertEquals(getKType<Promise<List<Bar<String>>>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of KType`() {
        val inquiry = Inquiry(getKType<Bar<String>>()).apply {
            wantsAsync = true
        }
        assertEquals(getKType<Bar<String>>(), inquiry.key)
        assertEquals(getKType<Promise<Bar<String>>>(), inquiry.resultType)
    }
}

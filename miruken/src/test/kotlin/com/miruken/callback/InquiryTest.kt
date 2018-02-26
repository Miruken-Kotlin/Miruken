package com.miruken.callback

import com.miruken.concurrent.Promise
import com.miruken.runtime.typeOf
import org.junit.Test
import kotlin.reflect.full.isSubtypeOf
import kotlin.test.*

class InquiryTest {
    class Foo
    class Bar<out T>

    @Test fun `Creates Inquiry of String`() {
        val inquiry = Inquiry("Service")
        assertFalse(inquiry.many)
        assertFalse(inquiry.wantsAsync)
        assertEquals("Service", inquiry.key)
        assertEquals(typeOf<Any>(), inquiry.resultType)
        assertSame(ProvidesPolicy, inquiry.policy)
    }

    @Test fun `Creates Inquiry of KClass`() {
        val inquiry = Inquiry(Foo::class)
        assertEquals(Foo::class, inquiry.key)
        assertEquals(typeOf<Foo>(), inquiry.resultType)
    }

    @Test fun `Creates Inquiry of generic KClass`() {
        val inquiry = Inquiry(Bar::class)
        assertEquals(Bar::class, inquiry.key)
        assertTrue(typeOf<Bar<*>>().isSubtypeOf(inquiry.resultType!!))
    }

    @Test fun `Creates Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>())
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(typeOf<Bar<String>>(), inquiry.resultType)
    }

    @Test fun `Creates many Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>(), true)
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(typeOf<List<Bar<String>>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of String`() {
        val inquiry = Inquiry("Service").apply { wantsAsync = true }
        assertTrue(inquiry.wantsAsync)
        assertEquals("Service", inquiry.key)
        assertEquals(typeOf<Promise<*>>(), inquiry.resultType)
        assertSame(ProvidesPolicy, inquiry.policy)
    }

    @Test fun `Creates async Inquiry of KClass`() {
        val inquiry = Inquiry(Foo::class).apply {
            wantsAsync = true
        }
        assertEquals(Foo::class, inquiry.key)
        assertEquals(typeOf<Promise<Foo>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of generic KClass`() {
        val inquiry = Inquiry(Bar::class).apply { wantsAsync = true }
        assertEquals(Bar::class, inquiry.key)
        assertTrue(typeOf<Promise<Bar<*>>>().isSubtypeOf(inquiry.resultType!!))
    }

    @Test fun `Creates async many Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>(), true).apply {
            wantsAsync = true
        }
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(typeOf<Promise<List<Bar<String>>>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>()).apply {
            wantsAsync = true
        }
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(typeOf<Promise<Bar<String>>>(), inquiry.resultType)
    }
}

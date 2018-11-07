package com.miruken.callback

import com.miruken.TypeReference
import com.miruken.concurrent.Promise
import com.miruken.kTypeOf
import com.miruken.typeOf
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
        assertEquals(TypeReference.ANY_STAR, inquiry.resultType)
        assertSame(ProvidesPolicy, inquiry.policy)
    }

    @Test fun `Creates Inquiry of KClass`() {
        val inquiry = Inquiry(Foo::class)
        assertEquals(Foo::class, inquiry.key)
        assertEquals(kTypeOf<Foo>(), inquiry.resultType)
    }

    @Test fun `Creates Inquiry of generic KClass`() {
        val inquiry = Inquiry(Bar::class)
        assertEquals(Bar::class, inquiry.key)
        assertTrue(kTypeOf<Bar<*>>().isSubtypeOf(inquiry.resultType!!))
    }

    @Test fun `Creates Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>())
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(kTypeOf<Bar<String>>(), inquiry.resultType)
    }

    @Test fun `Creates many Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>(), true)
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(kTypeOf<List<Bar<String>>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of String`() {
        val inquiry = Inquiry("Service").apply { wantsAsync = true }
        assertTrue(inquiry.wantsAsync)
        assertEquals("Service", inquiry.key)
        assertEquals(kTypeOf<Promise<*>>(), inquiry.resultType)
        assertSame(ProvidesPolicy, inquiry.policy)
    }

    @Test fun `Creates async Inquiry of KClass`() {
        val inquiry = Inquiry(Foo::class).apply {
            wantsAsync = true
        }
        assertEquals(Foo::class, inquiry.key)
        assertEquals(kTypeOf<Promise<Foo>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of generic KClass`() {
        val inquiry = Inquiry(Bar::class).apply { wantsAsync = true }
        assertEquals(Bar::class, inquiry.key)
        assertTrue(kTypeOf<Promise<Bar<*>>>().isSubtypeOf(inquiry.resultType!!))
    }

    @Test fun `Creates async many Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>(), true).apply {
            wantsAsync = true
        }
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(kTypeOf<Promise<List<Bar<String>>>>(), inquiry.resultType)
    }

    @Test fun `Creates async Inquiry of KType`() {
        val inquiry = Inquiry(typeOf<Bar<String>>()).apply {
            wantsAsync = true
        }
        assertEquals(typeOf<Bar<String>>(), inquiry.key)
        assertEquals(kTypeOf<Promise<Bar<String>>>(), inquiry.resultType)
    }
}

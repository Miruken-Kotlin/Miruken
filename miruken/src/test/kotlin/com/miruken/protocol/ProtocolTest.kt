package com.miruken.protocol

import org.junit.Test
import java.lang.reflect.Method
import kotlin.reflect.KType
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ProtocolTest {
    @Test  fun `Proxies through a protocol adapter`() {
        val email = Protocol.proxy<EmailFeature>(
                PassThroughAdapter(EmailFeatureImpl()))
        assertNotNull(email)
        val (confirmation, msg) = email.send("This is a test!")
        assertEquals(1, confirmation)
        assertEquals("This is a test!", msg)
    }

    @Test  fun `Rejects proxy if not protocol interface`() {
        assertFailsWith(IllegalArgumentException::class) {
            Protocol.proxy<EmailFeatureImpl>(
                    PassThroughAdapter(EmailFeatureImpl()))
        }
    }

    @Test  fun `Protocol proxies will propagate exceptions`() {
        assertFailsWith(IllegalArgumentException::class) {
            val email = Protocol.proxy<EmailFeature>(
                    PassThroughAdapter(EmailFeatureImpl()))
            email.send("")
        }
    }

    interface EmailFeature {
        fun send(message: String): Pair<Int, String>
    }

    class EmailFeatureImpl : EmailFeature {
        private var _count = 0

        override fun send(message: String): Pair<Int, String> {
            require(message.isNotEmpty(), { "Message cannot be blank" })
            return ++_count to message
        }
    }

    class PassThroughAdapter(private val target: Any) : ProtocolAdapter {
        override fun dispatch(
                protocol: KType,
                method:   Method,
                args:     Array<Any?>
        ): Any? = method.invoke(target, *args)
    }
}
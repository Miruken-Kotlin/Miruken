package com.miruken.protocol

import com.miruken.TypeReference
import org.junit.Test
import java.lang.reflect.Method
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

class ProtocolTest {
    @Test  fun `Proxies through a protocol adapter`() {
        val email = PassThroughAdapter(EmailFeatureImpl()).proxy<EmailFeature>()
        assertNotNull(email)
        val (confirmation, msg) = email.send("This is a test!")
        assertEquals(1, confirmation)
        assertEquals("This is a test!", msg)
    }

    @Test  fun `Rejects proxy if not protocol interface`() {
        assertFailsWith(IllegalArgumentException::class) {
            PassThroughAdapter(EmailFeatureImpl()).proxy<EmailFeatureImpl>()
        }
    }

    @Test  fun `Protocol proxies will propagate exceptions`() {
        assertFailsWith(IllegalArgumentException::class) {
            val email = PassThroughAdapter(EmailFeatureImpl())
                    .proxy<EmailFeature>()
            email.send("")
        }
    }

    interface EmailFeature {
        fun send(message: String): Pair<Int, String>
    }

    class EmailFeatureImpl : EmailFeature {
        private var _count = 0

        override fun send(message: String): Pair<Int, String> {
            require(message.isNotEmpty()) { "Message cannot be blank" }
            return ++_count to message
        }
    }

    class PassThroughAdapter(private val target: Any): ProtocolAdapter {
        override fun dispatch(
                protocol: TypeReference,
                method:   Method,
                args:     Array<Any?>
        ): Any? = method.invoke(target, *args)
    }
}
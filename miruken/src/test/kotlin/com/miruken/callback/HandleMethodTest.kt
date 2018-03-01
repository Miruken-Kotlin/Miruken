package com.miruken.callback

import com.miruken.protocol.proxy
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class HandleMethodTest {
    @Test fun `Handles method calls`() {
        val handler = EmailHandler()
        var id      = handler.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        id          = handler.proxy<EmailFeature>().email("Hello")
        assertEquals(2, id)
    }

    @Test fun `Handles property getters`() {
        val handler = EmailHandler()
        val count   = handler.proxy<EmailFeature>().count
        assertEquals(0, count)
    }

    @Test fun `Handles method calls covariantly`() {
        val handler = OfflineHandler()
        val id      = handler.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Handles method calls polymorphically`() {
        val handler = EmailHandler() + OfflineHandler()
        var id      = handler.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        id          = handler.proxy<EmailFeature>().email("Hello")
        assertEquals(2, id)
        id          = handler.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Handles method strictly`() {
        val handler = OfflineHandler()
        assertFailsWith(IllegalStateException::class) {
            handler.strict.proxy<EmailFeature>().email("22")
        }
    }

    @Test fun `Chains method calls strictly`() {
        val handler = OfflineHandler() + EmailHandler()
        val id      = handler.strict.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Requires protocol conformance`() {
        val handler = DemoHandler()
        assertFailsWith(IllegalStateException::class) {
            handler.proxy<EmailFeature>().email("22")
        }
    }

    @Test fun `Requires protocol invariance`() {
        val handler = DemoHandler()
        assertFailsWith(IllegalStateException::class) {
            handler.proxy<Offline>().email("22")
        }
    }

    @Test fun `Handles method calls without returns`() {
        val handler = EmailHandler() + BillingImpl()
        handler.proxy<EmailFeature>().cancelEmail(1)
    }

    @Test fun `Handles method calls using best effort`() {
        val handler = EmailHandler()
        val id      = handler.bestEffort.proxy<Offline>().email("Hello")
        assertEquals(0, id)
        var billing = handler.bestEffort.proxy<Offline>().billing
        assertNull(billing)
    }

    @Test fun `Should not propagate best effort calls`() {
        val handler = EmailHandler()
        assertFailsWith(IllegalStateException::class) {
            handler.bestEffort.proxy<EmailFeature>().cancelEmail(1)
        }
    }

    interface EmailFeature {
        val count: Int
        fun email(message: String): Int
        fun cancelEmail(id: Int)
    }

    @Resolving
    interface Billing {
        fun bill(amount:BigDecimal): BigDecimal
    }

    interface Offline : EmailFeature, Billing {
        val billing: Billing
    }

    class EmailHandler : Handler(), EmailFeature {
        override var count: Int = 0
            private set

        override fun email(message: String): Int {
            return if (count > 0 && count % 2 == 0)
                COMPOSER!!.proxy<Offline>().email(message)
            else ++count
        }

        override fun cancelEmail(id: Int) {
            val composer = COMPOSER!!
                    .takeUnless { id <= 4 } ?: COMPOSER!!
            composer.proxy<Billing>().bill(4.toBigDecimal())
        }
    }

    class BillingImpl(private val fee: BigDecimal) : Billing {
        constructor() : this(2.toBigDecimal())

        override fun bill(amount: BigDecimal): BigDecimal =
                amount + fee
    }

    class OfflineHandler : Handler(), Offline {
        override var count: Int = 0
            private set

        override val billing = BillingImpl()

        override fun email(message: String): Int {
            return ++count
        }

        override fun cancelEmail(id: Int) {}

        override fun bill(amount: BigDecimal): BigDecimal {
            throw IllegalStateException("Not supported offline")
        }
    }

    class DemoHandler : Handler() {
        fun email(message: String): Int {
            return Integer.parseInt(message)
        }

        fun bill(amount: BigDecimal): BigDecimal
        {
            return amount * 2.toBigDecimal()
        }
    }
}
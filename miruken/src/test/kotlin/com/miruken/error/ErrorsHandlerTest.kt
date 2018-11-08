package com.miruken.error

import com.miruken.test.assertAsync
import com.miruken.callback.Handler
import com.miruken.callback.plus
import com.miruken.concurrent.Promise
import com.miruken.protocol.proxy
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestName
import java.math.BigDecimal
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertNull

class ErrorsHandlerTest {
    @Rule
    @JvmField val testName = TestName()

    @Test fun `Suppress result by default`() {
        var handled = false
        val handler = ErrorsHandler()
        assertAsync(testName) { done ->
            Errors(handler).handleException(
                    IllegalArgumentException("This is bad")
            ).then  { handled = true }
             .catch { handled = true}
            done()
        }
        assertFalse(handled)
    }

    @Test fun `Recovers from exception`() {
        val handler = PaymentTech() + ErrorsHandler()
        assertFalse(handler.recover.proxy<Payments>().validateCard("1234"))
        val receipt: Receipt = handler.recover.proxy<Payments>()
                .getReceipt(UUID.randomUUID())
        assertNull(receipt)
    }

    @Test fun `Recovers from exception async`() {
        var handled = false
        val handler = PaymentTech() + ErrorsHandler()
        assertAsync { done ->
            handler.recover.proxy<Payments>()
                    .processPayments(10000.toBigDecimal()) then {
                handled = true
            } catch {
                handled = true
            } finally {
                done()
            }
        }
        assertFalse(handled)
    }

    @Test fun `Customizes exception handling`() {
        val handler  = (CustomErrorHandler()
                     + PaymentTech()
                     + ErrorsHandler())
        assertAsync(testName) { done ->
            handler.recover.proxy<Payments>()
                    .processPayments(1000.toBigDecimal()) then {
                done()
            }
        }
    }

    data class Receipt(val total: BigDecimal)

    interface Payments {
        fun validateCard(card: String): Boolean
        fun processPayments(payment: BigDecimal): Promise<UUID>
        fun getReceipt(confirmation: UUID): Receipt
    }

    class PaymentTech : Handler(), Payments {
        override fun validateCard(card: String): Boolean {
            require(card.length >= 10) {
                "Credit card must be at least 10 digits"
            }
            return true
        }

        override fun processPayments(payment: BigDecimal): Promise<UUID> {
            if (payment > 500.toBigDecimal())
                return Promise.reject(IllegalArgumentException(
                        "Amount exceeded limit"))
            return Promise.resolve(UUID.randomUUID())
        }

        override fun getReceipt(confirmation: UUID): Receipt {
            throw IllegalArgumentException("Confirmation $confirmation not found")
        }
    }

    class CustomErrorHandler : Handler(), Errors {
        override fun handleException(
                exception: Throwable,
                callback:  Any?,
                context:   Any?
        ) = Promise.resolve(UUID.randomUUID())
    }
}
@file:Suppress("UNUSED_PARAMETER")

package com.miruken.callback

import com.miruken.api.StashImpl
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.kTypeOf
import com.miruken.protocol.proxy
import com.miruken.runtime.checkOpenConformance
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.reflect.KType
import kotlin.reflect.KTypeParameter
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HandleMethodTest {
    @Before
    fun setup() {
        HandlerDescriptorFactory.useFactory(
            MutableHandlerDescriptorFactory().apply {
                registerDescriptor<EmailHandler>()
                registerDescriptor<OfflineHandler>()
                registerDescriptor<DemoHandler>()
                registerDescriptor<BillingImpl>()
                registerDescriptor<StashImpl>()
            })
    }

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
        assertFailsWith<NotHandledException> {
            handler.strict.proxy<EmailFeature>().email("22")
        }
    }

    @Test fun `Handles method loosely`() {
        val handler = DemoHandler()
        val id      = handler.duck.proxy<EmailFeature>().email("22")
        assertEquals(22, id)
    }

    @Test fun `Chains method calls strictly`() {
        val handler = OfflineHandler() + EmailHandler()
        val id      = handler.strict.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Requires protocol conformance`() {
        val handler = DemoHandler()
        assertFailsWith<NotHandledException> {
            handler.proxy<EmailFeature>().email("22")
        }
    }

    @Test fun `Requires protocol invariance`() {
        val handler = DemoHandler()
        assertFailsWith<NotHandledException> {
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
        val billing = handler.bestEffort.proxy<Offline>().billing
        assertNull(billing)
    }

    @Test fun `Should not propagate best effort calls`() {
        val handler = EmailHandler()
        assertFailsWith<NotHandledException> {
            handler.bestEffort.proxy<EmailFeature>().cancelEmail(1)
        }
    }

    @Test fun `Applies nested best effort`() {
        val handler = EmailHandler()
        handler.bestEffort.proxy<EmailFeature>().cancelEmail(6)
    }

    @Test fun `Broadcasts method calls`() {
        val master = EmailHandler()
        val mirror = EmailHandler()
        val backup = EmailHandler()
        assertEquals(0, master.count)
        assertEquals(0, mirror.count)
        assertEquals(0, backup.count)
        val email  = master + mirror + backup
        val id     = email.broadcast.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        assertEquals(1, master.count)
        assertEquals(1, mirror.count)
        assertEquals(1, backup.count)
    }

    @Test fun `Rejects unhandled method calls`() {
        assertFailsWith<NotHandledException> {
            Handler().proxy<EmailFeature>().email("Hello")
        }
    }

    @Test fun `Rejects unhandled method calls broadcast`() {
        assertFailsWith<NotHandledException> {
            Handler().broadcast.proxy<EmailFeature>().email("Hello")
        }
    }

    @Test fun `Resolves methods calls inferred`() {
        val handler = EmailHandler()
        val id      = handler.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Resolves methods calls implicitly`() {
        val handler = BillingImpl().toHandler()
        val total   = handler.proxy<Billing>().bill(7.5.toBigDecimal())
        assertEquals(9.5.toBigDecimal(), total)
    }

    @Test fun `Does not resolve methods calls implicitly`() {
        val handler = DemoHandler()
        assertFailsWith<NotHandledException> {
            handler.proxy<Billing>().bill(15.toBigDecimal())
        }
    }

    @Test fun `Handles methods calls using protocol`() {
        val handler = BillingImpl(4.toBigDecimal()).toHandler()
        val total   = handler.proxy<Billing>().bill(3.toBigDecimal())
        assertEquals(7.toBigDecimal(), total)
    }

    @Test fun `Handles default method calls`() {
        val handler = EmailHandler()
        val id      = handler.proxy<EmailFeature>().email(19)
        assertEquals(1, id)
    }

    @Test fun `Aborts method calls`() {
        assertFailsWith<NotHandledException> {
            EmailHandler().proxy<EmailFeature>().email("Abort")
        }
    }

    @Test fun `Checks filter open conformance`() {
        val openType = Filtering::class.createType(listOf(
                KTypeProjection.contravariant(kTypeOf<HandleMethod>()),
                KTypeProjection.invariant(
                        Filtering::class.typeParameters[1].createType()))
        )
        val otherType = kTypeOf<Log2Filter<Int>>()
        val bindings  = mutableMapOf<KTypeParameter, KType>()
        assertNotNull(openType.checkOpenConformance(otherType, bindings))
        assertEquals(1, bindings.size)
        assertEquals(kTypeOf<Int>(), bindings.values.first())
    }

    interface EmailFeature {
        val count: Int
        fun email(message: String): Int
        fun cancelEmail(id: Int)
        fun <T: Any> email(message: T) =
                email(message.toString())
    }

    @Log
    @Resolving
    interface Billing {
        fun bill(amount:BigDecimal): BigDecimal
    }

    @Log
    interface Offline : EmailFeature, Billing {
        val billing: Billing
    }

    @Abort
    class EmailHandler : Handler(), EmailFeature {
        @get:Log
        @set:Log
        override var count: Int = 0
            private set

        @Log
        override fun email(message: String): Int {
            return if (count > 0 && count % 2 == 0)
                COMPOSER!!.proxy<Offline>().email(message)
            else ++count
        }

        @Log
        override fun cancelEmail(id: Int) {
            val composer = COMPOSER!!
                    .takeIf { id <= 4 } ?: COMPOSER!!.bestEffort
            composer.proxy<Billing>().bill(4.toBigDecimal())
        }

        @Provides
        fun <Res> createLogger(inquiry: Inquiry): LogFilter<Res>?
        {
            @Suppress("UNCHECKED_CAST")
            return when (inquiry.keyClass) {
                LogFilter::class -> LogFilter()
                else -> null
            }
        }

        @Provides
        fun <R: Any?> createAborting(inquiry: Inquiry) =
                AbortFilter<R>()
    }

    @Log
    class BillingImpl(private val fee: BigDecimal) : Billing {
        constructor() : this(2.toBigDecimal())

        override fun bill(amount: BigDecimal): BigDecimal =
                amount + fee

        @Provides
        fun <Res> createLogger(inquiry: Inquiry): LogFilter<Res>?
        {
            @Suppress("UNCHECKED_CAST")
            return when (inquiry.keyClass) {
                LogFilter::class -> LogFilter()
                else -> null
            }
        }
    }

    @Log2
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

        @Provides
        fun <Res: Any?> createLogger(
                inquiry: Inquiry
        ): Log2Filter<Res> = Log2Filter()
    }

    @Log2
    class DemoHandler : Handler() {
        fun email(message: String): Int {
            return Integer.parseInt(message)
        }

        fun bill(amount: BigDecimal): BigDecimal {
            return amount * 2.toBigDecimal()
        }

        @Provides
        @Suppress("UNCHECKED_CAST")
        fun <Res: Any?> createLogger(inquiry: Inquiry) =
                inquiry.createKeyInstance() as? Filtering<HandleMethod, Res>
    }

    class LogFilter<Res> : Filtering<HandleMethod, Res> {
        override var order: Int? = 1

        override fun next(
                callback:    HandleMethod,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Res>,
                provider:    FilteringProvider?
        ): Promise<Res> {
            print("Handle method '${callback.method.name}' with result ")
            val result = next()
            println(result)
            return result
        }
    }


    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.PROPERTY_SETTER)
    @UseFilter(LogFilter::class)
    annotation class Log

    class Log2Filter<Res> : Filtering<HandleMethod, Res> {
        override var order: Int? = 2

        override fun next(
                callback:    HandleMethod,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<Res>,
                provider:    FilteringProvider?
        ): Promise<Res> {
            println("Log2 method '${callback.method.name}'")
            return next()
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER)
    @UseFilter(Log2Filter::class)
    annotation class Log2

    class AbortFilter<R: Any?> : Filtering<HandleMethod, R> {
        override var order: Int? = 0

        override fun next(
                callback:    HandleMethod,
                rawCallback: Any,
                binding:     MemberBinding,
                composer:    Handling,
                next:        Next<R>,
                provider:    FilteringProvider?
        ) = when {
            callback.method.name == "email" &&
                    callback.arguments[0] == "Abort" -> next.abort()
            else -> next()
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION,
            AnnotationTarget.PROPERTY_GETTER)
    @UseFilter(AbortFilter::class)
    annotation class Abort
}
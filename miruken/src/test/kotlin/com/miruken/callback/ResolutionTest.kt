package com.miruken.callback

import com.miruken.callback.policy.HandlerDescriptor
import com.miruken.callback.policy.MemberBinding
import com.miruken.protocol.proxy
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ResolutionTest {
    @Test fun `Overrides providers`() {
        val demo    = DemoHandler()
        val handler = Handler()
        val resolve = handler.resolving.provide(demo).resolve<DemoHandler>()
        assertSame(demo, resolve)
    }

    @Test fun `Overrides providers polymorphically`() {
        val email   = EmailHandler()
        val handler = Handler()
        val resolve = handler.resolving.provide(email).resolve<EmailFeature>()
        assertSame(email, resolve)
    }

    @Test fun `Overrides providers resolving`() {
        HandlerDescriptor.getDescriptorFor<DemoProvider>()
        val demo    = DemoHandler()
        val handler = Handler()
        val resolve = handler.provide(demo).resolving.resolve<DemoHandler>()
        assertSame(demo, resolve)
    }

    @Test fun `Resolves handlers`() {
        HandlerDescriptor.getDescriptorFor<EmailHandler>()
        val handler = EmailProvider()
        val id      = handler.resolving.command(SendEmail("Hello")) as Int
        assertEquals(1, id)
    }

    @Test fun `Resolves implied handlers`() {
        val handler = EmailHandler()
        val id      = handler.resolving.command(SendEmail("Hello")) as Int
        assertEquals(1, id)
    }

    @Test fun `Resolves all handlers`() {
        HandlerDescriptor.getDescriptorFor<EmailHandler>()
        HandlerDescriptor.getDescriptorFor<OfflineHandler>()
        val handler = EmailProvider() + OfflineProvider()
        val id      = handler.resolvingAll.command(SendEmail("Hello")) as Int
        assertEquals(1, id)
    }

    @Test fun `Resolves implied open-generic handlers`() {
        val handler = Repository<Message>()
        val message = Message()
        val result  = handler.resolving.handle(Create(message))
        assertEquals(HandleResult.HANDLED, result)
    }

    @Test fun `Resolves handlers with filters`() {
        HandlerDescriptor.getDescriptorFor<EmailHandler>()
        val handler = (EmailProvider()
                    + BillingImpl()
                    + RepositoryProvider()
                    + FilterProvider())
        val id = handler.resolving.command(SendEmail("Hello")) as Int
        assertEquals(10, id)
    }

    @Test fun `Resolves filters with generic constraints`() {
        val handler = (Accountant()
                    + BillingImpl()
                    + RepositoryProvider()
                    + FilterProvider())
        val balance = handler.resolving.command(
                Create(Deposit().apply { amount = 10.toBigDecimal() }))
                as BigDecimal
        assertEquals(13.toBigDecimal(), balance)
    }

    @Test fun `Skips filters with missing dependencies`() {
        val handler = Accountant() + FilterProvider()
        handler.resolving.command(
                Create(Deposit().apply { amount = 10.toBigDecimal() }))
    }

    @Test fun `Fails if no handlers resolved`() {
        val handler = BillingImpl().toHandler()
        assertFailsWith(NotHandledException::class) {
            handler.resolving.command(SendEmail("Hello"))
        }
    }

    @Test fun `Provides methods`() {
        val provider = EmailProvider()
        var id       = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        id           = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(2, id)
    }

    @Test fun `Provides properties`() {
        val provider = EmailProvider()
        val count    = provider.proxy<EmailFeature>().count
        assertEquals(0, count)
    }

    @Test fun `Provides methods covariantly`() {
        val provider = OfflineProvider()
        val id       = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Provides methods polymorphically`() {
        val provider = EmailProvider() + OfflineProvider()
        var id       = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        id           = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(2, id)
        id       = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Provides methods strictly`() {
        val provider = OfflineProvider()
        assertFailsWith(NotHandledException::class) {
            provider.strict.proxy<EmailFeature>().email("22")
        }
    }

    @Test fun `Chains Provided methods strictly`() {
        val provider = OfflineProvider() + EmailProvider()
        val id       = provider.strict.proxy<EmailFeature>().email("22")
        assertEquals(1, id)
    }

    @Test fun `Requires protocol conformance`() {
        val provider = DemoProvider()
        assertFailsWith(NotHandledException::class) {
            provider.duck.proxy<EmailFeature>().email("22")
        }
    }

    @Test fun `Requires protocol invariance`() {
        val provider = DemoProvider()
        assertFailsWith(NotHandledException::class) {
            provider.proxy<Offline>().email("22")
        }
    }

    @Test fun `Provides methods with no return value`() {
        val provider = EmailProvider() + BillingProvider(BillingImpl())
        provider.proxy<EmailFeature>().cancelEmail(1)
    }

    @Test fun `Visits all providers`() {
        val provider = ManyProvider()
        provider.proxy<EmailFeature>().cancelEmail(13)
    }

    @Test fun `Ignores unhandled methods`() {
        val provider = OfflineHandler()
        assertFailsWith(NotHandledException::class) {
            provider.proxy<Offline>().cancelEmail(13)
        }
    }

    @Test fun `Finds matching method`() {
        val provider = OfflineHandler() + EmailProvider()
        assertFailsWith(NotHandledException::class) {
            provider.proxy<Offline>().cancelEmail(13)
        }
    }

    @Test fun `Provides methods best effort`() {
        val provider = Handler()
        val id       = provider.bestEffort.proxy<EmailFeature>().email("Hello")
        assertEquals(0, id)
    }

    @Test fun `Does not propagate best effort`() {
        val provider = EmailProvider()
        assertFailsWith(NotHandledException::class) {
            provider.proxy<Offline>().cancelEmail(1)
        }
    }

    @Test fun `Applies nested best effort`() {
        val provider = EmailProvider()
        provider.bestEffort.proxy<EmailFeature>().cancelEmail(6)
    }

    @Test fun `Broadcasts method calls`() {
        val master = EmailProvider()
        val mirror = EmailProvider()
        val backup = EmailProvider()
        val email  = master + mirror + backup
        val id     = email.broadcast.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        assertEquals(1, master.resolve<EmailHandler>()!!.count)
        assertEquals(1, mirror.resolve<EmailHandler>()!!.count)
        assertEquals(1, backup.resolve<EmailHandler>()!!.count)
    }

    @Test fun `Resolves methods calls inferred`() {
        val provider = EmailProvider()
        val id       = provider.resolving.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Resolves methods calls implicitly`() {
        val provider = BillingProvider(BillingImpl())
        val total    = provider.proxy<Billing>().bill(7.5.toBigDecimal())
        assertEquals(9.5.toBigDecimal(), total)
    }

    @Test fun `Does not resolve methods implicitly`() {
        val provider = DemoProvider()
        assertFailsWith(NotHandledException::class) {
            provider.proxy<Billing>().bill(15.toBigDecimal())
        }
    }

    @Test fun `Handles methods calls using protocol`() {
        val provider = BillingProvider(BillingImpl(4.toBigDecimal()))
        val total    = provider.proxy<Billing>().bill(3.toBigDecimal())
        assertEquals(7.toBigDecimal(), total)
    }

    interface Entity {
        var id: Int
    }

    @Resolving
    interface EmailFeature {
        val count: Int
        fun email(message: String): Int
        fun cancelEmail(id: Int)
    }

    @Resolving
    interface Billing {
        fun bill(amount: BigDecimal): BigDecimal
    }

    interface Offline : EmailFeature, Billing {
        val billing: Billing
    }

    class SendEmail<out T>(val body: T)

    class Message : Entity {
        override var id: Int = 0
        var content: String  = ""
    }

    class Deposit : Entity {
        override var id: Int   = 0
        var amount: BigDecimal = 0.toBigDecimal()
    }

    class Withdrawal : Entity {
        override var id: Int   = 0
        var amount: BigDecimal = 0.toBigDecimal()
    }

    class Create<out T: Entity>(val entity: T)

    @Audit
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
                    .takeIf { id <= 4 } ?: COMPOSER!!.bestEffort
            composer.proxy<Billing>().bill(4.toBigDecimal())
        }

        @Handles
        fun <T> send(send: SendEmail<T>): Int {
            return email(send.body.toString())
        }
    }

    class BillingImpl(private val fee: BigDecimal) : Billing {
        constructor() : this(2.toBigDecimal())

        override fun bill(amount: BigDecimal) = amount + fee
    }

    class OfflineHandler : Handler(), Offline {
        override var count: Int = 0
            private set

        override val billing = BillingImpl()

        override fun email(message: String): Int {
            return ++count
        }

        override fun cancelEmail(id: Int) {
            if (id == 13)
                notHandled()
        }

        override fun bill(amount: BigDecimal): BigDecimal {
            throw IllegalStateException("Not supported offline")
        }

        @Handles
        fun <T> send(send: SendEmail<T>): Int {
            return email(send.body.toString())
        }
    }

    class DemoHandler : Handler() {
        fun email(message: String): Int {
            return Integer.parseInt(message)
        }

        fun bill(amount: BigDecimal): BigDecimal {
            return amount * 2.toBigDecimal()
        }
    }

    class EmailProvider : Handler() {
        @Provides
        val provideEmail = EmailHandler()
    }

    class BillingProvider(@Provides val billing: Billing) : Handler()

    class DemoProvider : Handler() {
        @Provides
        val provideDemo = DemoHandler()
    }

    class OfflineProvider : Handler() {
        @Provides
        val provideOffline = OfflineHandler()
    }

    class ManyProvider : Handler() {
        @Provides
        fun provideEmail(): List<EmailFeature> =
                listOf(OfflineHandler(), EmailHandler())
    }

    class Repository<in T: Entity> : Handler() {
        private var _nextId = 1

        @Handles
        fun create(create: Create<T>) {
            create.entity.id = _nextId++
        }
    }

    class Accountant : Handler() {
        private var _balance: BigDecimal = 0.toBigDecimal()

        @Handles
        @Balance
        fun depositFunds(deposit: Create<Deposit>): BigDecimal {
            _balance += deposit.entity.amount
            return _balance
        }

        @Handles
        @Balance
        fun withdrawFunds(withdraw: Create<Withdrawal>): BigDecimal {
            _balance -= withdraw.entity.amount
            return _balance
        }
    }

    @Suppress("UNCHECKED_CAST")
    class RepositoryProvider : Handler() {
        @Provides
        fun <T: Entity> createRepository(inquiry: Inquiry): Repository<T>? {
            return inquiry.createKeyInstance() as? Repository<T>
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(AuditFilter::class)
    annotation class Audit

    class AuditFilter<in Cb: Any, Res: Any?> : DynamicFilter<Cb, Res>() {
        fun next(
                callback:       Cb,
                next:           Next<Res>,
                binding:        MemberBinding,
                repository:     Repository<Message>,
                @Proxy billing: Billing
        ): Res {
            @Suppress("UNCHECKED_CAST")
            (callback as? SendEmail<*>)?.also {
                (it.body as? String)?.also { body ->
                    val message = Message().apply { content = body }
                    repository.create(Create(message))
                    if (binding.returnType.classifier == Int::class) {
                        billing.bill(message.id.toBigDecimal())
                        return (message.id * 10) as Res
                    }
                }
            }
            return next()
        }
    }

    @Target(AnnotationTarget.CLASS,AnnotationTarget.FUNCTION)
    @UseFilter(BalanceFilter::class)
    annotation class Balance

    class BalanceFilter<T: Entity, Res: Number> : DynamicFilter<Create<T>, Res>() {
        fun next(callback:   Create<T>,
                 next:       Next<Res>,
                 binding:    MemberBinding,
                 repository: Repository<T>,
                 billing:    Billing
        ): Res {
            println("Balance for $callback")
            repository.create(callback)
            if (binding.returnType.classifier == BigDecimal::class) {
                @Suppress("UNCHECKED_CAST")
                return ((next() as BigDecimal) +
                    billing.bill(callback.entity.id.toBigDecimal())) as Res
            }
            return next()
        }
    }

    class FilterProvider : Handler() {
        @Provides
        fun <Cb: Any, Res: Any?> providesAudit() = AuditFilter<Cb, Res>()

        @Provides
        fun <T: Entity, Res: Number> providesBalance() = BalanceFilter<T, Res>()
    }
}
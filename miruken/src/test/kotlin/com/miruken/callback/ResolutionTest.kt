package com.miruken.callback

import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.Promise
import com.miruken.protocol.proxy
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class ResolutionTest {
    private lateinit var factory: HandlerDescriptorFactory

    @Before
    fun setup() {
        factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<EmailHandler>()
        factory.registerDescriptor<EmailProvider>()
        factory.registerDescriptor<OfflineHandler>()
        factory.registerDescriptor<DemoHandler>()
        factory.registerDescriptor<BillingProvider>()
        factory.registerDescriptor<OfflineProvider>()
        factory.registerDescriptor<DemoProvider>()
        factory.registerDescriptor<ManyProvider>()
        factory.registerDescriptor<FilterProvider>()
        factory.registerDescriptor<Repository<*>>()
        factory.registerDescriptor<RepositoryProvider>()
        factory.registerDescriptor<Accountant>()
        factory.registerDescriptor<Provider>()
        HandlerDescriptorFactory.useFactory(factory)
    }

    @Test fun `Overrides providers`() {
        val demo    = DemoHandler()
        val handler = Handler()
        val resolve = handler.infer.provide(demo).resolve<DemoHandler>()
        assertSame(demo, resolve)
    }

    @Test fun `Overrides providers polymorphically`() {
        val email   = EmailHandler()
        val handler = Handler()
        val resolve = handler.infer.provide(email).resolve<EmailFeature>()
        assertSame(email, resolve)
    }

    @Test fun `Overrides providers resolving`() {
        factory.registerDescriptor<DemoProvider>()
        val demo    = DemoHandler()
        val handler = Handler()
        val resolve = handler.provide(demo).infer.resolve<DemoHandler>()
        assertSame(demo, resolve)
    }

    @Test fun `Resolves handlers`() {
        factory.registerDescriptor<EmailHandler>()
        val handler = (EmailProvider()
                    + BillingImpl()
                    + RepositoryProvider()
                    + FilterProvider())
        val id      = handler.infer.command(SendEmail("Hello")) as Int
        assertEquals(10, id)
    }

    @Test fun `Resolves implied handlers`() {
        val handler = (EmailHandler()
                    + BillingImpl()
                    + RepositoryProvider()
                    + FilterProvider())
        val id      = handler.infer.command(SendEmail("Hello")) as Int
        assertEquals(10, id)
    }

    @Test fun `Resolves all handlers`() {
        factory.registerDescriptor<EmailHandler>()
        factory.registerDescriptor<OfflineHandler>()
        val handler = EmailProvider() + OfflineProvider()
        val id      = handler.inferAll.command(SendEmail("Hello")) as Int
        assertEquals(1, id)
    }

    @Test fun `Resolves implied open-generic handlers`() {
        val handler = Repository<Message>()
        val message = Message()
        val result  = handler.infer.handle(Create(message))
        assertEquals(HandleResult.HANDLED, result)
    }

    @Test fun `Resolves handlers with filters`() {
        factory.registerDescriptor<EmailHandler>()
        val handler = (EmailProvider()
                    + BillingImpl()
                    + RepositoryProvider()
                    + FilterProvider())
        val id = handler.infer.command(SendEmail("Hello")) as Int
        assertEquals(10, id)
    }

    @Test fun `Resolves filters with generic constraints`() {
        val handler = (Accountant()
                    + BillingImpl()
                    + RepositoryProvider()
                    + FilterProvider())
        val balance = handler.infer.command(
                Create(Deposit().apply { amount = 10.toBigDecimal() }))
                as BigDecimal
        assertEquals(13.toBigDecimal(), balance)
    }

    @Test fun `Skips filters with missing dependencies`() {
        val handler = (Accountant()
                    + BillingImpl()
                    + RepositoryProvider()
                    + FilterProvider())
        handler.infer.command(
                Create(Deposit().apply { amount = 10.toBigDecimal() }))
    }

    @Test fun `Fails if no handlers resolved`() {
        val handler = BillingImpl().toHandler()
        assertFailsWith(NotHandledException::class) {
            handler.infer.command(SendEmail("Hello"))
        }
    }

    @Test fun `Provides methods`() {
        val provider = (EmailProvider()
                     +  BillingImpl()
                     +  RepositoryProvider()
                     +  FilterProvider())
        var id       = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        id           = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(2, id)
    }

    @Test fun `Provides properties`() {
        val provider = (EmailProvider()
                     +  BillingImpl()
                     +  RepositoryProvider()
                     +  FilterProvider())
        val count    = provider.proxy<EmailFeature>().count
        assertEquals(0, count)
    }

    @Test fun `Provides methods covariantly`() {
        val provider = OfflineProvider()
        val id       = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Provides methods polymorphically`() {
        val provider = (EmailProvider()
                     + OfflineProvider()
                     + RepositoryProvider()
                     + FilterProvider())
        var id       = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        id           = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(2, id)
        id           = provider.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
    }

    @Test fun `Provides methods strictly`() {
        val provider = (OfflineProvider()
                     + BillingImpl()
                     + RepositoryProvider()
                     + FilterProvider())
        assertFailsWith(NotHandledException::class) {
            provider.strict.proxy<EmailFeature>().email("22")
        }
    }

    @Test fun `Chains provided methods strictly`() {
        val provider = (OfflineProvider()
                     + EmailProvider()
                     + BillingImpl()
                     + RepositoryProvider()
                     + FilterProvider())
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
        val provider = (EmailProvider()
                     + BillingProvider(BillingImpl())
                     + RepositoryProvider()
                     + FilterProvider())
        provider.proxy<EmailFeature>().cancelEmail(1)
    }

    @Test fun `Visits all providers`() {
        val provider = (ManyProvider()
                     + BillingImpl()
                     + RepositoryProvider()
                     + FilterProvider())
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
        val email  = (master + mirror + backup
                   + RepositoryProvider()
                   + FilterProvider())
        val id     = email.broadcast.proxy<EmailFeature>().email("Hello")
        assertEquals(1, id)
        assertEquals(1, master.resolve<EmailHandler>()!!.count)
        assertEquals(1, mirror.resolve<EmailHandler>()!!.count)
        assertEquals(1, backup.resolve<EmailHandler>()!!.count)
    }

    @Test fun `Resolves methods calls inferred`() {
        val provider = (EmailProvider()
                     +  BillingImpl()
                     +  RepositoryProvider()
                     +  FilterProvider())
        val id       = provider.infer.proxy<EmailFeature>().email("Hello")
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
        @get:Provides
        val provideEmail = EmailHandler()
    }

    class BillingProvider(@get:Provides val billing: Billing) : Handler()

    class DemoProvider : Handler() {
        @get:Provides
        val provideDemo = DemoHandler()
    }

    class OfflineProvider : Handler() {
        @get:Provides
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
        ): Promise<Res> {
            @Suppress("UNCHECKED_CAST")
            (callback as? SendEmail<*>)?.also {
                (it.body as? String)?.also { body ->
                    val message = Message().apply { content = body }
                    repository.create(Create(message))
                    if (binding.returnType.classifier == Int::class) {
                        billing.bill(message.id.toBigDecimal())
                        return Promise.resolve((message.id * 10)) as Promise<Res>
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
        ): Promise<Res> {
            println("Balance for $callback")
            repository.create(callback)
            if (binding.returnType.classifier == BigDecimal::class) {
                @Suppress("UNCHECKED_CAST")
                return next() then {
                    ((it as BigDecimal) + billing.bill(
                            callback.entity.id.toBigDecimal())) as Res
                }
            }
            return next()
        }
    }

    class FilterProvider : Handler() {
        @Provides
        fun <Cb: Any, Res: Any?> providesAudit():
                AuditFilter<Cb, Res> = AuditFilter()

        @Provides
        fun <T: Entity, Res: Number> providesBalance():
                BalanceFilter<T, Res> = BalanceFilter()
    }
}
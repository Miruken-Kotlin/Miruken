package com.miruken.context

import com.miruken.callback.*
import com.miruken.callback.policy.HandlerDescriptorFactory
import com.miruken.callback.policy.MutableHandlerDescriptorFactory
import com.miruken.callback.policy.registerDescriptor
import com.miruken.concurrent.ChildCancelMode
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState
import com.miruken.protocol.proxy
import org.junit.Before
import org.junit.Test
import kotlin.test.*

@Suppress("RemoveExplicitTypeArguments", "UNUSED_VARIABLE")
class ContextTest {
    @Before
    fun setup() {
        val factory = MutableHandlerDescriptorFactory()
        factory.registerDescriptor<Gateway>()
        factory.registerDescriptor<Observer>()
        HandlerDescriptorFactory.useFactory(factory)
    }
    @Test fun `Starts in the active state`() {
        val context = Context()
        assertEquals(ContextState.ACTIVE, context.state)
    }

    @Test fun `Resolves self when requested`() {
        val context = Context()
        assertSame(context, context.resolve<Context>())
    }

    @Test fun `Root context has no parent`() {
        val context = Context()
        assertNull(context.parent)
    }

    @Test fun `Gets root context`() {
        val context = Context()
        val child   = context.createChild()
        assertSame(context, context.root)
        assertSame(context, child.root)
    }

    @Test fun `Children should have a parent`() {
        val context = Context()
        val child   = context.createChild()
        assertSame(context, child.parent)
    }

    @Test fun `Has no children initially`() {
        val context = Context()
        assertFalse(context.hasChildren)
    }

    @Test fun `Has children if created`() {
        val context = Context()
        val child1  = context.createChild()
        val child2  = context.createChild()
        assertTrue(context.hasChildren)
        assertEquals(listOf(child1, child2), context.children)
    }

    @Test fun `Can end a context`() {
        val context = Context()
        context.end()
        assertEquals(ContextState.ENDED, context.state)
    }

    @Test fun `Ends child contexts when ended`() {
        val context = Context()
        val child   = context.createChild()
        assertEquals(ContextState.ACTIVE, child.state)
        context.end()
        assertEquals(ContextState.ENDED, child.state)
    }

    @Test fun `Ends context if closed`() {
        val context = Context()
        context.close()
        assertEquals(ContextState.ENDED, context.state)
    }

    @Test fun `Can unwind children`() {
        val context = Context()
        val child1  = context.createChild()
        val child2  = context.createChild()
        context.unwind()
        assertEquals(ContextState.ACTIVE, context.state)
        assertEquals(ContextState.ENDED, child1.state)
        assertEquals(ContextState.ENDED, child2.state)
    }

    @Test fun `Can unwind to root`() {
        val context    = Context()
        val child1     = context.createChild()
        val child2     = context.createChild()
        val grandchild = child1.createChild()
        val root       = child2.unwindToRoot()
        assertSame(context, root)
        assertEquals(ContextState.ACTIVE, context.state)
        assertEquals(ContextState.ENDED, child1.state)
        assertEquals(ContextState.ENDED, child2.state)
        assertEquals(ContextState.ENDED, grandchild.state)
    }

    @Test fun `Can store anything`() {
        val data    = Foo()
        val context = Context()
        context.store(data)
        assertSame(data, context.resolve<Foo>())
    }

    @Test fun `Traverses ancestors by default`() {
        val data       = Foo()
        val context    = Context()
        val child      = context.createChild()
        val grandchild = child.createChild()
        context.store(data)
        assertSame(data, grandchild.resolve<Foo>())
    }

    @Test fun `Traverses self`() {
        val data  = Foo()
        val root  = Context()
        val child = root.createChild()
        root.store(data)
        assertNull(child.xself.resolve<Foo>())
        assertSame(data, root.xself.resolve<Foo>())
    }

    @Test fun `Traverses root`() {
        val data  = Foo()
        val root  = Context()
        val child = root.createChild()
        child.store(data)
        assertNull(child.xroot.resolve<Foo>())
        root.store(data)
        assertSame(data, root.xroot.resolve<Foo>())
    }

    @Test fun `Traverses children`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        child2.store(data)
        assertNull(child2.xchild.resolve<Foo>())
        assertNull(grandChild.xchild.resolve<Foo>())
        assertSame(data, root.xchild.resolve<Foo>())
    }

    @Test fun `Traverses siblings`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        child3.store(data)
        assertNull(root.xsibling.resolve<Foo>())
        assertNull(child3.xsibling.resolve<Foo>())
        assertNull(grandChild.xsibling.resolve<Foo>())
        assertSame(data, child2.xsibling.resolve<Foo>())
    }

    @Test fun `Traverses self or children`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        child3.store(data)
        assertNull(child1.xselfOrChild.resolve<Foo>())
        assertNull(grandChild.xselfOrChild.resolve<Foo>())
        assertSame(data, child3.xselfOrChild.resolve<Foo>())
        assertSame(data, root.xselfOrChild.resolve<Foo>())
    }

    @Test fun `Traverses self or siblings`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        child3.store(data)
        assertNull(root.xselfOrSibling.resolve<Foo>())
        assertNull(grandChild.xselfOrSibling.resolve<Foo>())
        assertSame(data, child3.xselfOrSibling.resolve<Foo>())
        assertSame(data, child2.xselfOrSibling.resolve<Foo>())
    }

    @Test fun `Traverses ancestors`() {
        val data       = Foo()
        val root       = Context()
        val child      = root.createChild()
        val grandChild = child.createChild()
        root.store(data)
        assertNull(root.xancestor.resolve<Foo>())
        assertSame(data, grandChild.xancestor.resolve<Foo>())
    }

    @Test fun `Traverses self or ancestors`() {
        val data       = Foo()
        val root       = Context()
        val child      = root.createChild()
        val grandChild = child.createChild()
        root.store(data)
        assertSame(data, root.xselfOrAncestor.resolve<Foo>())
        assertSame(data, grandChild.xselfOrAncestor.resolve<Foo>())
    }

    @Test fun `Traverses descendants`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        grandChild.store(data)
        assertNull(grandChild.xdescendant.resolve<Foo>())
        assertNull(child2.xdescendant.resolve<Foo>())
        assertSame(data, child3.xdescendant.resolve<Foo>())
        assertSame(data, root.xdescendant.resolve<Foo>())
    }

    @Test fun `Traverses descendants or self`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        grandChild.store(data)
        assertNull(child2.xselfOrDescendant.resolve<Foo>())
        assertSame(data, grandChild.xselfOrDescendant.resolve<Foo>())
        assertSame(data, child3.xselfOrDescendant.resolve<Foo>())
        assertSame(data, root.xselfOrDescendant.resolve<Foo>())
    }

    @Test fun `Traverses self or descendants`() {
        val data = Foo()
        val root = Context()
        val child1 = root.createChild()
        val child2 = root.createChild()
        val child3 = root.createChild()
        var grandChild = child3.createChild()
        root.store(data)
        assertNull(child2.xselfOrDescendant.resolve<Foo>())
        assertSame(data, root.xselfOrDescendant.resolve<Foo>())
    }

    @Test fun `Traverses self, siblings or ancestors`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        child2.store(data)
        assertNull(grandChild.xselfSiblingOrAncestor.resolve<Foo>())
        assertSame(data, child3.xselfSiblingOrAncestor.resolve<Foo>())
    }

    @Test fun `Traverses ancestors, self or siblings`() {
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        child3.store(data)
        assertSame(data, grandChild.xselfSiblingOrAncestor.resolve<Foo>())
    }

    @Test fun `Publishes from root context`() {
        val data       = Foo()
        val root       = Context()
                .apply { Observer().context = this }
        val child1     = root.createChild()
                .apply { Observer().context = this }
        val child2     = root.createChild()
                .apply { Observer().context = this }
        val child3     = root.createChild()
                .apply { Observer().context = this }
        val grandChild = child3.createChild()
                .apply { Observer().context = this }
        child2.publishFromRoot.handle(data)
        assertEquals(5, data.count)
    }

    @Test fun `Rejects publish if no root context`() {
        assertFailsWith(IllegalStateException::class) {
            Handler().publishFromRoot.handle(Foo())
        }
    }

    @Test fun `Combines aspects with traversal`() {
        var count      = 0
        val data       = Foo()
        val root       = Context()
        val child1     = root.createChild()
        val child2     = root.createChild()
        val child3     = root.createChild()
        val grandChild = child3.createChild()
        grandChild.store(data)

        fun foo(h: Handling): Handling = h.aspectBefore({ _,_ -> ++count })

        assertNull(foo(child2.xselfOrDescendant).resolve<Foo>())
        assertSame(data, foo(grandChild.xselfOrDescendant).resolve<Foo>())
        assertSame(data, foo(child3.xselfOrDescendant).resolve<Foo>())
        assertSame(data, foo(root.xselfOrDescendant).resolve<Foo>())
        assertSame(4, count)
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun `Cancels promise when context ends`() {
        val foo     = Foo()
        val root    = Context()
        val promise = Gateway().commandAsync(foo) as Promise<Foo>
        assertEquals(PromiseState.FULFILLED, promise.state)
        assertSame(foo, promise.get())
        assertEquals(1, foo.count)

        val promise2 = Gateway().trackPromise(root)
                .commandAsync(foo) as Promise<Foo>
        assertEquals(PromiseState.PENDING, promise2.state)
        assertEquals(2, foo.count)
        root.end()
        assertEquals(PromiseState.CANCELLED, promise2.state)
        assertEquals(10, foo.count)

        val promise3 = Gateway().trackPromise(root)
                .commandAsync(foo) as Promise<Foo>
        assertEquals(PromiseState.FULFILLED, promise3.state)
        assertSame(foo, promise.get())
        assertEquals(11, foo.count)

        val promise4 = Gateway().trackPromise(root)
                .commandAsync(foo) as Promise<Foo>
        assertEquals(PromiseState.CANCELLED, promise4.state)
        assertEquals(10, foo.count)
    }

    @Suppress("UNCHECKED_CAST")
    @Test fun `Cancels proxy promise when context ends`() {
        val foo     = Foo()
        val root    = Context().apply {
            addHandlers(Gateway())
        }
        val promise = root.proxy<Api>().send(foo)
        assertEquals(PromiseState.FULFILLED, promise.state)
        assertSame(foo, promise.get())
        assertEquals(1, foo.count)

        val promise2 = root.async.proxy<Api>().send(foo)
        assertEquals(PromiseState.PENDING, promise2.state)
        assertEquals(2, foo.count)
        root.end()
        assertEquals(PromiseState.CANCELLED, promise2.state)
        assertEquals(10, foo.count)

        val promise3 = root.async.proxy<Api>().send(foo)
        assertEquals(PromiseState.FULFILLED, promise3.state)
        assertSame(foo, promise.get())
        assertEquals(11, foo.count)

        val promise4 = root.async.proxy<Api>().send(foo)
        assertEquals(PromiseState.CANCELLED, promise4.state)
        assertEquals(10, foo.count)
    }

    class Foo {
        var count = 0
    }

    class Observer : ContextualHandler() {
        @Handles
        fun handle(foo: Foo, composer: Handling) {
            val ctx = composer.resolve<Context>()
            assertSame(context!!.root, ctx)
            ++foo.count
        }
    }

    interface Api {
        fun send(foo: Foo): Promise<Foo>
    }

    class Gateway : Handler(), Api {
        @Handles
        fun handle(foo: Foo): Promise<Foo> {
            ++foo.count
            return if (foo.count % 2 == 0) {
                Promise<Foo>(ChildCancelMode.ANY) { _, _, cancel ->
                    cancel { foo.count = 10 }
                }
            } else {
                Promise.resolve(foo)
            }
        }

        override fun send(foo: Foo) = handle(foo)
    }
}
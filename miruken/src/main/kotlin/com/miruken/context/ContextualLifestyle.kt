package com.miruken.context

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.ConstraintProvider
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.bindings.Qualifier
import com.miruken.concurrent.Promise
import java.util.concurrent.ConcurrentHashMap

class ContextualLifestyle<Res>(val rooted: Boolean) : Lifestyle<Res>() {
    override fun isCompatibleWithParent(parent: Inquiry) =
            parent.dispatcher?.let { dispatcher ->
                dispatcher.filterProviders
                        .filterIsInstance<LifestyleProvider>()
                        .all {
                            it is ContextualLifestyleProvider &&
                                    (rooted || !it.rooted)
                        }
            } ?: true

    override fun getInstance(
            inquiry:  Inquiry,
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Promise<Res>? {
        val context = composer.resolve<Context>()?.let {
            if (rooted) it.root else it
        } ?: return null
        @Suppress("UNCHECKED_CAST")
        return Promise.resolve(_cache.getOrPut(context) {
            val instance = next().get()
            if (instance is Contextual) {
                instance.context = context
                val undo = instance.contextChanging.register(::changeContext)
                context.contextEnded += { event ->
                    _cache.remove(event.context)
                    undo()
                    (instance as? AutoCloseable)?.close()
                    instance.context = null
                }
            } else {
                context.contextEnded += { event ->
                    _cache.remove(event.context)
                    (instance as? AutoCloseable)?.close()
                }
            }
            instance
        } as Any) as Promise<Res>
    }

    private fun changeContext(event: ContextChangingEvent) {
        val oldContext = event.oldContext
        val newContext = event.newContext
        if (oldContext == newContext) return
        check(newContext == null) {
            "Managed instances cannot change context"
        }
        _cache[oldContext]
                ?.takeIf { it === event.contextual }?.also {
                    _cache.remove(oldContext)
                    (it as? AutoCloseable)?.close()
                }
    }

    private val _cache = ConcurrentHashMap<Context, Res>()
}

object ContextualLifestyleProvider : LifestyleProvider() {
    var rooted = false
        private set

    override fun configure(owner: Any) {
        if (owner is Scoped) {
            rooted = owner.rooted
        }
    }

    override fun createLifestyle() = ContextualLifestyle<Any>(rooted)
}

object ScopeQualifierProvider : ConstraintProvider(
        Qualifier(Scoped::class)
)

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER)
@UseFilterProvider(
        ContextualLifestyleProvider::class,
        ScopeQualifierProvider::class)
annotation class Scoped(val rooted: Boolean = false)
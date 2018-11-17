package com.miruken.context

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.ConstraintProvider
import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.callback.policy.bindings.Qualifier
import java.util.concurrent.ConcurrentHashMap

class ContextualLifestyle<Res> : Lifestyle<Res>() {
    override fun getInstance(
            inquiry:  Inquiry,
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Res? {
        if (!checkCompatibleParent(inquiry.parent)) {
            return null
        }
        val context = composer.resolve<Context>() ?: return null
        return _cache.getOrPut(context) {
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
        }
    }

    private fun changeContext(event: ContextChangingEvent) {
        val oldContext = event.oldContext
        val newContext = event.newContext
        if (oldContext == newContext) return
        check(newContext == null) {
            "Managed instances cannot change context"
        }
        _cache[oldContext]?.takeIf { it === event.contextual }?.also {
            _cache.remove(oldContext)
            (it as? AutoCloseable)?.close()
        }
    }

    private fun checkCompatibleParent(parent: Inquiry?) =
            parent?.dispatcher?.let { dispatcher ->
                dispatcher.filterProviders
                        .all { it is ContextualLifestyleProvider}
            } ?: true

    private val _cache = ConcurrentHashMap<Context, Res>()
}

object ContextualLifestyleProvider : LifestyleProvider({
    ContextualLifestyle<Any>()
})

object ScopeQualifierProvider : ConstraintProvider(
        Qualifier(Scoped::class)
)

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY)
@UseFilterProvider(
        ContextualLifestyleProvider::class,
        ScopeQualifierProvider::class)
annotation class Scoped
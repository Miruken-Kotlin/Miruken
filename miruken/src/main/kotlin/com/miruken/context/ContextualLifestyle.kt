package com.miruken.context

import com.miruken.callback.*
import com.miruken.callback.policy.bindings.MemberBinding
import java.util.concurrent.ConcurrentHashMap

class ContextualLifestyle<Res>: Lifestyle<Res>() {
    override fun getInstance(
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Res? {
        val context = composer.resolve<Context>() ?: return null
        return _cache.getOrPut(context) {
            val instance = next().get()
            if (instance is Contextual) {
                instance.context = context
                context.removeHandlers(instance)
                val undo = instance.contextChanging.register(::changeContext)
                context.contextEnded += { event ->
                    _cache.remove(event.context)
                    undo()
                    instance.context = null
                    (instance as? AutoCloseable)?.close()
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

    private val _cache = ConcurrentHashMap<Context, Res>()
}

object ContextualLifestyleProvider : LifestyleProvider({
    ContextualLifestyle<Any>()
})

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY)
@UseFilterProvider(ContextualLifestyleProvider::class)
annotation class Scoped
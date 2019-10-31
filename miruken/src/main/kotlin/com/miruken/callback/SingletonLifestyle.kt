package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise

class SingletonLifestyle<Res> : Lifestyle<Res>() {
    @Volatile
    private var _instance: Res? = null

    override fun isCompatibleWithParent(parent: Inquiry) = true

    override fun getInstance(
            inquiry:  Inquiry,
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Promise<Res>? {
        var instance = _instance
        if (instance == null) {
            synchronized(this) {
                instance = _instance
                if (instance == null) {
                    instance = next().get()
                    _instance = instance
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return instance?.let {
            Promise.resolve(it as Any) as Promise<Res>
        }
    }
}

object SingletonLifestyleProvider : LifestyleProvider() {
    override fun createLifestyle() = SingletonLifestyle<Any>()
}

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY_GETTER)
@UseFilterProvider(SingletonLifestyleProvider::class)
annotation class Singleton
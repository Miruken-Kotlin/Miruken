package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise

class SingletonLifestyle<Res> : Lifestyle<Res>() {
    @Volatile
    private var _instance: Promise<Res>? = null

    override fun isCompatibleWithParent(parent: Inquiry) = true

    override fun getInstance(
            inquiry:  Inquiry,
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Promise<Res>? {
        val val1 = _instance
        if (val1 != null) return val1
        return synchronized(this) {
            val val2 = _instance
            if (val2 != null) val2
            else {
                val instance = next()
                _instance = instance
                instance
            }
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
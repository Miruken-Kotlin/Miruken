package com.miruken.callback

import com.miruken.callback.policy.MemberBinding

class SingletonLifestyle<Res> : Lifestyle<Res>() {
    @Volatile
    private var _instance: Res? = null

    override fun getInstance(
            binding:  MemberBinding,
            next:     Next<Res>,
            composer: Handling
    ): Res? {
        val val1 = _instance
        if (val1 != null) return val1
        return synchronized(this) {
            val val2 = _instance
            if (val2 != null) val2
            else {
                val instance = next().get()
                _instance = instance
                instance
            }
        }
    }
}

object SingletonLifestyleProvider : LifestyleProvider({
    SingletonLifestyle<Any>()
})

@Target(AnnotationTarget.CONSTRUCTOR, AnnotationTarget.FUNCTION,
        AnnotationTarget.PROPERTY)
@UseFilterProvider(SingletonLifestyleProvider::class)
annotation class Singleton
package com.miruken.callback

import com.miruken.callback.policy.bindings.MemberBinding
import com.miruken.concurrent.Promise
import com.miruken.concurrent.PromiseState

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
        if (_instance == null) {
            synchronized(this) {
                if (_instance == null) {
                    val result = next()
                    when (result.state) {
                        PromiseState.FULFILLED -> {
                            _instance = result.get()
                            return result
                        }
                        PromiseState.REJECTED,
                        PromiseState.CANCELLED ->
                            return result
                        else ->
                            _instance = result.get()
                    }
                }
            }
        }
        @Suppress("UNCHECKED_CAST")
        return _instance?.let {
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
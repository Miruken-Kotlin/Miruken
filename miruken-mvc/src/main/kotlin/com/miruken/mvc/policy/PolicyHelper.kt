package com.miruken.mvc.policy

import com.miruken.callback.Callback
import com.miruken.callback.Handling
import com.miruken.callback.filter
import com.miruken.concurrent.Promise

fun Handling.trackPromise(policyOwner: PolicyOwner<*>) =
        trackPromise(policyOwner.policy)

fun Handling.trackPromise(parentPolicy: Policy) =
        filter { callback, _, _, proceed ->
            proceed().also { result ->
                result success {
                    val cb = callback as? Callback
                    (cb?.result as? Promise<*>)?.also {
                        val dependency = PromisePolicy(it).autoRelease()
                        parentPolicy.addDependency(dependency)
                        it.finally {
                            parentPolicy.removeDependency(dependency)
                        }
                    }
                }
            }
        }
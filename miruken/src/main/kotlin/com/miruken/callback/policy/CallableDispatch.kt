package com.miruken.callback.policy

import com.miruken.TypeFlags
import com.miruken.TypeInfo
import com.miruken.callback.Handling
import com.miruken.callback.Strict
import com.miruken.callback.getFilterProviders
import com.miruken.callback.resolve
import com.miruken.concurrent.Promise
import com.miruken.concurrent.asPromise
import com.miruken.runtime.isNothing
import com.miruken.runtime.isUnit
import com.miruken.runtime.requiresReceiver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import java.lang.reflect.InvocationTargetException
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KAnnotatedElement
import kotlin.reflect.KCallable
import kotlin.reflect.full.callSuspend
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.isAccessible
import kotlin.reflect.jvm.jvmErasure

class CallableDispatch(val callable: KCallable<*>) : KAnnotatedElement {
    val strict     = annotations.any { it is Strict }
    val returnInfo = TypeInfo.parse(callable.returnType)
    val arguments  = callable.valueParameters.map(::Argument)

    init { callable.isAccessible = true }

    inline   val arity        get() = arguments.size
    inline   val owningClass  get() = owningType.jvmErasure
    inline   val returnType   get() = callable.returnType
    inline   val isContructor get() = !callable.requiresReceiver
    override val annotations  get() = callable.annotations

    val owningType get() = callable.instanceParameter?.type
            ?: callable.returnType

    val filterProviders by lazy { getFilterProviders() }

    val returnsSomething get() =
        !returnType.isUnit && !returnType.isNothing

    fun invoke(
            receiver:  Any,
            arguments: Array<Any?>,
            composer:  Handling
    ) = try {
            if (callable.requiresReceiver) {
                if (callable.isSuspend) {
                    getCoroutineScope(composer).async {
                        callable.callSuspend(receiver, *arguments)
                    }.asPromise()
                } else {
                    callable.call(receiver, *arguments)
                }
            } else if (callable.isSuspend) {
                getCoroutineScope(composer).async {
                    callable.callSuspend(*arguments)
                }.asPromise()
            } else {
                callable.call(*arguments)
            }
        } catch (e: Throwable) {
            val cause = (e as? InvocationTargetException)?.cause ?: e
            if (returnInfo.flags has TypeFlags.PROMISE) {
                Promise.reject(cause)
            } else {
                throw cause
            }
        }

    private fun getCoroutineScope(composer: Handling) =
            CoroutineScope((composer.resolve<CoroutineContext>()
                    ?: EmptyCoroutineContext) + SupervisorJob())
}
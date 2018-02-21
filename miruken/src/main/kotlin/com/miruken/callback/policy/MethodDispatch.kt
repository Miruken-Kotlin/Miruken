package com.miruken.callback.policy

import kotlin.reflect.KCallable

class MethodDispatch(val callable: KCallable<*>) {
    val annotations get() = callable.annotations
}
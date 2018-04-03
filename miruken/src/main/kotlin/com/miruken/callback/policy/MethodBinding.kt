package com.miruken.callback.policy

import java.lang.reflect.Method
import kotlin.reflect.KType

abstract class MethodBinding(val method: Method?) {
    abstract val returnType: KType
}
package com.miruken.callback.policy.bindings

import com.miruken.callback.FilteredObject
import kotlin.reflect.KType

abstract class MemberBinding : FilteredObject() {
    abstract val returnType:  KType
    abstract val skipFilters: Boolean
}
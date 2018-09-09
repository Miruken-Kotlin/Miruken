package com.miruken.callback.policy

import com.miruken.callback.FilteredObject
import java.lang.reflect.Member
import kotlin.reflect.KType

abstract class MemberBinding(val member: Member) : FilteredObject() {
    abstract val returnType: KType

    abstract val skipFilters: Boolean
}
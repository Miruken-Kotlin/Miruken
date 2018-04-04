package com.miruken.callback.policy

import java.lang.reflect.Member
import kotlin.reflect.KType

abstract class MemberBinding(val member: Member) {
    abstract val returnType: KType
}
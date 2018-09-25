package com.miruken.callback

import com.miruken.callback.policy.MemberBinding
import kotlin.reflect.KClass

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class UseFilter(
        val filterClass: KClass<out Filtering<*,*>>,
        val many:        Boolean = false,
        val order:       Int = -1,
        val required:    Boolean = false)

interface UseFilterValidating {
    fun validate(filter: UseFilter, binding: MemberBinding)
}

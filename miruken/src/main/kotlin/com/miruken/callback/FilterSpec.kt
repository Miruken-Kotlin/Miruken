package com.miruken.callback

import kotlin.reflect.KClass

data class FilterSpec(
        val filterby: KClass<out Filtering<*,*>>,
        val order:    Int?    = null,
        val required: Boolean = false
)
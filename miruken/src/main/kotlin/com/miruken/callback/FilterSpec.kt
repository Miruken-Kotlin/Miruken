package com.miruken.callback

import kotlin.reflect.KClass

data class FilterSpec(
        val filterby: KClass<out Filtering<*, *>>,
        val many:     Boolean = false,
        val order:    Int?    = null,
        val required: Boolean = false
)
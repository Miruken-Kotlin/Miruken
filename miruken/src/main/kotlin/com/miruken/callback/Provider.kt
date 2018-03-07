package com.miruken.callback

import com.miruken.runtime.componentType
import com.miruken.runtime.isCompatibleWith
import kotlin.reflect.KType

class Provider(
        private val value: Any,
        type: KType? = null
) : Handler() {

    private var _key: Any = type?.componentType ?: value::class

    @Provides
    fun provide(inquiry: Inquiry): Any? =
            if (isCompatibleWith(inquiry.key, _key)) value else null
}
package com.miruken.callback

import com.miruken.runtime.componentType
import kotlin.reflect.KType

class Provider(val value: Any, type: KType? = null) : Handler() {
    private var _componentType: KType? = type?.componentType

    @Provides
    fun provide(inquiry: Inquiry) : Any? {
        val key = inquiry.key
        return when (key) {
            is KType -> null
            else -> null
        }
    }
}
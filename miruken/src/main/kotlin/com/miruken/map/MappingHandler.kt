package com.miruken.map

import com.miruken.callback.COMPOSER
import com.miruken.callback.Handler
import com.miruken.callback.notHandled
import com.miruken.concurrent.Promise
import kotlin.reflect.KType

class MappingHandler : Handler(), Mapping {
    override fun map(
            source:     Any,
            targetType: KType,
            sourceType: KType?,
            target:     Any?,
            format:     Any?
    ): Any? {
        val mapFrom = MapFrom(source, targetType,
                sourceType, target, format)
        return COMPOSER!!.handle(mapFrom) success { mapFrom.result }
                ?: notHandled()
    }

    override fun mapAsync(
            source:     Any,
            targetType: KType,
            sourceType: KType?,
            target:     Any?,
            format:     Any?
    ): Promise<Any?> {
        val mapFrom = MapFrom(source, targetType,
                sourceType, target, format).apply {
            wantsAsync = true
        }
        return COMPOSER!!.handle(mapFrom) success {
            @Suppress("UNCHECKED_CAST")
            mapFrom.result as? Promise<Any?>
        } ?: notHandled()
    }
}
package com.miruken.api.resource

import org.threeten.bp.LocalDateTime

interface Resource<TId> {
    val id:         TId?
    val rowVersion: ByteArray?
    val created:    LocalDateTime?
    val createdBy:  String?
    val modified:   LocalDateTime?
    val modifiedBy: String?
}

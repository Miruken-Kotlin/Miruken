package com.miruken.api.resource

import java.time.LocalDateTime

interface Resource<TId> {
    val id:         TId?
    val rowVersion: ByteArray?
    val created:    LocalDateTime?
    val createdBy:  String?
    val modified:   LocalDateTime?
    val modifiedBy: String?
}

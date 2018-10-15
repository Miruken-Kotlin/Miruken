package com.miruken.callback.policy.rules

import kotlin.reflect.KType

class RuleContext {
    private var _aliases: MutableMap<String, KType>? = null
    private var _errors: MutableList<String>? = null

    val hasErrors get() =_errors?.isNotEmpty() ?: false
    val errors get() = _errors?.toList() ?: emptyList()

    fun addAlias(alias: String, type: KType): Boolean {
        if (_aliases == null) {
            _aliases = mutableMapOf(alias to type)
            return true
        }
        val aliasedType = _aliases!![alias]
        if (aliasedType != null) {
            if (aliasedType != type) {
                addError("Mismatched alias '$alias', $type != $aliasedType")
                return false
            }
        } else {
            _aliases!![alias] = type
        }
        return true
    }

    fun addError(error: String) {
        if (_errors == null)
            _errors = mutableListOf()
        _errors!!.add(error)
    }
}
package com.miruken.mvc

class GoBack {
    private var _result: Any? = null

    fun setResult(result: Any?) {
        _result = result
    }

    fun clearResult(): Any? {
        val result = _result
        _result    = null
        return result
    }
}
package com.miruken.validate

interface ScopeMatching {
    fun matches(scope: Any?): Boolean
}

class EqualsScopeMatcher(val scope: Any?) : ScopeMatching {
    override fun matches(scope: Any?): Boolean {
        if (scope == Scopes.ANY || this.scope == Scopes.ANY)
            return true
        return when (scope) {
            is Collection<*> -> scope.contains(this.scope)
            is Array<*> -> scope.contains(this.scope)
            else -> scope == this.scope
        }
    }

    companion object {
        val DEFAULT = EqualsScopeMatcher(Scopes.DEFAULT)
    }
}

class CompositeScopeMatcher(
        private val matchers: Collection<ScopeMatching>
) : ScopeMatching {
    override fun matches(scope: Any?) =
        matchers.any { it.matches(scope) }
}
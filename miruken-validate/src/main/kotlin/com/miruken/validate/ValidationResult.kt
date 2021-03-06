package com.miruken.validate

sealed class ValidationResult {
    abstract val error: String
    abstract val isValid: Boolean

    override fun toString() = error

    data class Error(
            override val error: String,
            val validatorName:  String? = null
    ) : ValidationResult() {
        override val isValid = false
    }

    class Outcome : ValidationResult() {
        private val _errors = mutableMapOf<String, Errors>()
        private var _errorDetails = lazy(_errors) { error }

        override val isValid get() = _errors.isEmpty()
        val culprits: Collection<String> get() = _errors.keys

        fun getCulprits(validatorName: String) =
                _errors.filter {
                    it.value.errors?.any { err ->
                        err.validatorName == validatorName } == true
                }.map { it.key }

        override val error: String
            get() = synchronized(_errors) {
                _errors.map { (key, err) ->
                    ("$key | " + (err.errors?.let { e ->
                        e.joinToString("; ") { it.error } +
                                (err.cascade?.run { "\n" + error } ?: "")
                    } ?: (err.cascade?.error ?: "")))
                }.joinToString("\n")
            }

        operator fun get(propertyName: String): String {
            val (key, outcome) = parse(propertyName)
            return when (outcome) {
                this -> synchronized(_errors) {
                    _errors[key]?.let { err ->
                        err.errors?.let { e ->
                            e.joinToString("; ") { it.error } +
                                    (err.cascade?.run { "\n" + error } ?: "")
                        } ?: (err.cascade?.error ?: "")
                    } ?: ""
                }
                null -> ""
                else -> outcome[key]
            }
        }

        fun getResults(propertyName: String): List<ValidationResult> {
            val (key, outcome) = parse(propertyName)
            return when (outcome) {
                this -> synchronized(_errors) {
                    _errors[key]?.results ?: emptyList()
                }
                null -> emptyList()
                else -> outcome.getResults(key)
            }
        }

        fun getOutcome(propertyName: String): Outcome? {
            val (key, outcome) = parse(propertyName)
            return when (outcome) {
                this -> getOrCreateOutcome(key)
                else -> outcome?.getOutcome(key)
            }
        }

        fun addError(propertyName: String, error: String) =
                addResult(propertyName, Error(error))

        fun addResult(propertyName: String, result: ValidationResult): Outcome {
            if (result.isValid) return this
            val (key, outcome) = parse(propertyName, true)
            when (outcome) {
                this -> synchronized(_errors) {
                    if (_errorDetails.isInitialized()) {
                        _errorDetails = lazy(_errors) { error }
                    }
                    _errors.getOrPut(key) { Errors() }
                            .addResult(result)
                }
                else -> outcome!!.addResult(key, result)
            }
            return this
        }

        fun merge(outcome: Outcome): Outcome {
            synchronized(outcome._errors) {
                if (outcome.isValid) return this
                for ((key, results) in outcome._errors) {
                    results.errors?.forEach { addResult(key, it) }
                    results.cascade?.also { addResult(key, it) }
                }
            }
            return this
        }

        private fun getOrCreateOutcome(
                propertyName: String,
                create:       Boolean = false
        ): Outcome? {
            return if (create) {
                _errors.getOrPut(propertyName) { Errors() }.let {
                    it.cascade ?: Outcome().apply { it.cascade = this }
                }
            } else {
                _errors[propertyName]?.cascade
            }
        }

        private fun parse(
                propertyName: String,
                create:       Boolean = false
        ): Pair<String, Outcome?> {
            val key = propertyName.trim().trimStart('.')
            require(key.isNotEmpty()) {
                "Property name must not be empty"
            }
            val index = key.indexOfAny(charArrayOf('.','['))
            return when {
                index < 0 -> key to this
                index == 0 -> {
                    val end = key.indexOf(']', index + 1)
                    if (end > index + 1 ) {
                        val own = key.substring(index + 1, end)
                        if (key.length == end + 1) {
                            return own to this
                        } else {
                            getOrCreateOutcome(own, create)?.let {
                                return propertyName.substring(end + 2) to it
                            }
                        }
                    }
                    key to null
                }
                else -> {
                    val own = key.substring(0, index)
                    getOrCreateOutcome(own, create)?.let {
                        val start = if (key[index] == '.')
                            index + 1 else index
                        key.substring(start) to it
                    } ?: key to null
                }
            }
        }
    }

    private data class Errors(
            var cascade: Outcome?            = null,
            var errors:  MutableList<Error>? = null
    ) {
        val results: List<ValidationResult>
            get() = errors?.toList() ?: emptyList<ValidationResult>() +
                (cascade?.let { listOf<ValidationResult>(it) }
                    ?: emptyList())

        fun addResult(result: ValidationResult) {
            when (result) {
                is Error ->
                    (errors ?: mutableListOf<Error>()
                            .apply { errors = this })
                            .add(result)
                is Outcome ->
                    cascade?.apply { merge(result) }
                        ?: result.also { cascade = it }
            }
        }
    }
}

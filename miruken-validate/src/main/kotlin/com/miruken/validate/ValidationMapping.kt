package com.miruken.validate

import com.miruken.callback.Handler
import com.miruken.map.FormatType
import com.miruken.map.Maps

@Suppress("ArrayInDataClass")
data class ValidationErrors(
    val propertyName: String,
    val errors:       Array<String>?           = null,
    val nested:       Array<ValidationErrors>? = null
)

class ValidationMapping : Handler() {
    @Maps
    @FormatType(Exception::class)
    fun map(exception: ValidationException) =
            createErrors(exception.outcome)

    @Maps
    @FormatType(Exception::class)
    fun map(errors: Array<ValidationErrors>) =
            ValidationException(createOutcome(errors))

    private fun createOutcome(
            errors: Array<ValidationErrors>
    ): ValidationResult.Outcome = ValidationResult.Outcome().apply {
        errors.forEach { property ->
            val propertyName = property.propertyName
            property.errors?.forEach { error ->
                addError(propertyName, error)
            }
            property.nested?.takeIf { it.isNotEmpty() }?.also {
                addResult(propertyName, createOutcome(it))
            }
        }
    }

    private fun createErrors(
            outcome: ValidationResult.Outcome
    ): Array<ValidationErrors> = outcome.culprits.map { culprit ->
        val messages = mutableListOf<String>()
        val children = mutableListOf<ValidationErrors>()
        outcome.getResults(culprit).forEach { result ->
            when (result) {
                is ValidationResult.Outcome ->
                    children.addAll(createErrors(result))
                is ValidationResult.Error ->
                    messages.add(result.error)
            }
        }
        ValidationErrors(culprit,
                messages.takeUnless { it.isEmpty() }?.toTypedArray(),
                children.takeUnless { it.isEmpty() }?.toTypedArray()
                )
    }.toTypedArray()
}
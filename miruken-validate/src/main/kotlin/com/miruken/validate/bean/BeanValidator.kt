package com.miruken.validate.bean

import com.miruken.callback.Handler
import com.miruken.validate.Validates
import com.miruken.validate.Validation
import com.miruken.validate.scopes.Anything
import javax.validation.Validator

class BeanValidator(
        private val validator: Validator
) : Handler() {
    @Validates(Anything::class)
    fun <T> validate(target: T, validation: Validation) {
        val outcome = validation.outcome
        validator.validate(target).forEach {
            val propertyName = it.propertyPath.toString()
            outcome.addError(propertyName, it.message)
        }
    }
}
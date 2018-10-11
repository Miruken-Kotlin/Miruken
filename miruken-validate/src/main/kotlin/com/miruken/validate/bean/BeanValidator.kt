package com.miruken.validate.bean

import com.miruken.callback.Handler
import com.miruken.callback.Handling
import com.miruken.validate.Validates
import com.miruken.validate.Validation
import com.miruken.validate.scopes.Everything
import javax.validation.ValidatorFactory

class BeanValidator(
        private val validatorFactory: ValidatorFactory
) : Handler() {
    @Validates(Everything::class)
    fun <T> validate(
            target:     T,
            validation: Validation,
            composer:   Handling
    ) {
        val outcome = validation.outcome
        val scopes  = validation.scopes.map { it.java }.toTypedArray()
        val context = validatorFactory.usingContext()
        context.constraintValidatorFactory(
                ComposerConstraintValidatorFactoy(context, composer,
                        validatorFactory.constraintValidatorFactory))
        context.validator.validate(target, *scopes).forEach {
            val propertyName = it.propertyPath.toString()
            outcome.addError(propertyName, it.message)
        }
    }
}
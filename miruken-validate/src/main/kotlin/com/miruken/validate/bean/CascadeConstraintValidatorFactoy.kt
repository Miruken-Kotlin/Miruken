package com.miruken.validate.bean

import com.miruken.callback.Handling
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorFactory
import javax.validation.ValidatorContext

class CascadeConstraintValidatorFactoy(
        private val validatorContext:           ValidatorContext,
        private val composer:                   Handling,
        private val constraintValidatorFactory: ConstraintValidatorFactory
): ConstraintValidatorFactory {
    override fun <T : ConstraintValidator<*,*>?> getInstance(key: Class<T>?): T {
        val constraintValidator = constraintValidatorFactory.getInstance(key)
        (constraintValidator as? CascadeAware)
                ?.setCascadeInfo(validatorContext, composer)
        return constraintValidator
    }

    override fun releaseInstance(instance: ConstraintValidator<*,*>?) {
        constraintValidatorFactory.releaseInstance(instance)
    }
}
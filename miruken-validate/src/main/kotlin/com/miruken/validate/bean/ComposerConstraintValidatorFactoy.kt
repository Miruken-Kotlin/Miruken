package com.miruken.validate.bean

import com.miruken.callback.ComposerAware
import com.miruken.callback.Handling
import javax.validation.ConstraintValidator
import javax.validation.ConstraintValidatorFactory
import javax.validation.ValidatorContext

class ComposerConstraintValidatorFactoy(
        private val validatorContext:           ValidatorContext,
        private val composer:                   Handling,
        private val constraintValidatorFactory: ConstraintValidatorFactory
): ConstraintValidatorFactory {
    override fun <T : ConstraintValidator<*,*>?>
            getInstance(key: Class<T>?): T {
        val constraintValidator = constraintValidatorFactory.getInstance(key)
        (constraintValidator as? ComposerAware)
                ?.setComposer(composer)
        return constraintValidator
    }

    override fun releaseInstance(instance: ConstraintValidator<*,*>?) {
        constraintValidatorFactory.releaseInstance(instance)
    }
}
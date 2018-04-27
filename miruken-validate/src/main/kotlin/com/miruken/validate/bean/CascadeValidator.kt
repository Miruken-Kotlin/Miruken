package com.miruken.validate.bean

import com.miruken.callback.Handling
import com.miruken.validate.Validating
import com.miruken.validate.ValidationResult
import javax.validation.*
import kotlin.reflect.KClass

@MustBeDocumented
@Constraint(validatedBy = [
    CascadeCollectionValidator::class,
    CascadeValidator::class])
@Target(AnnotationTarget.FUNCTION,AnnotationTarget.FIELD,
        AnnotationTarget.CONSTRUCTOR,AnnotationTarget.VALUE_PARAMETER,
        AnnotationTarget.TYPE_PARAMETER)
annotation class Valid(
        val groups:  Array<KClass<out Any>> = [],
        val payload: Array<KClass<out Any>> = [],
        val message: String = ""
)

class CascadeCollectionValidator : ConstraintValidator<Valid, Collection<*>> {
    override fun isValid(value: Collection<*>?, context: ConstraintValidatorContext?): Boolean {
        return true
    }

}

class CascadeValidator : ConstraintValidator<Valid, Any>, CascadeAware {
    private lateinit var _groups: Array<KClass<out Any>>
    private var _validationContext: ValidatorContext? = null
    private var _composer:          Handling?         = null

    override fun initialize(constraintAnnotation: Valid) {
        _groups = constraintAnnotation.groups
    }

    override fun setCascadeInfo(
            validatorContext: ValidatorContext,
            composer: Handling
    ) {
        _validationContext = validatorContext
        _composer          = composer
    }

    override fun isValid(
            value:   Any?,
            context: ConstraintValidatorContext
    ): Boolean {
        if (value == null || _validationContext == null || _composer == null) {
            return true
        }

        val validating  = Validating(_composer!!)
        val validator   = _validationContext!!.validator
        val constraints = validator.getConstraintsForClass(value.javaClass)

        context.disableDefaultConstraintViolation()
        val builder = context.buildConstraintViolationWithTemplate(
                context.defaultConstraintMessageTemplate)

        when {
            constraints.isBeanConstrained -> {
                val outcome = validating.validate(value, null, *_groups)
                if (!outcome.isValid) {
                    addErrors(outcome, builder)
                }
            }
            value is Iterable<*> -> {
                value.forEachIndexed { index, element ->
                    if (element != null &&
                            validator.getConstraintsForClass(element.javaClass)
                                    .isBeanConstrained) {
                        val outcome = validating.validate(element, null, *_groups)
                        if (!outcome.isValid) {
                            addErrors(outcome, builder, index)
                        }
                    }
                }
            }
            value is Array<*> -> {
                value.forEachIndexed { index, element ->
                    if (element != null &&
                            validator.getConstraintsForClass(element.javaClass)
                                    .isBeanConstrained) {
                        val outcome = validating.validate(element, null, *_groups)
                        if (!outcome.isValid) {
                            addErrors(outcome, builder, index)
                        }
                    }
                }
            }
        }

        return false
    }

    private fun addErrors(
            outcome: ValidationResult.Outcome,
            builder: ConstraintValidatorContext.ConstraintViolationBuilder,
            index:   Int? = null
    ) {
        outcome.culprits.forEach { propertyName ->
            outcome.getResults(propertyName).forEach { error ->
                when (error) {
                    is ValidationResult.Error -> {
                        builder.addPropertyNode(propertyName)
                    }
                    is ValidationResult.Outcome -> {

                    }
                }
            }
        }
    }
}
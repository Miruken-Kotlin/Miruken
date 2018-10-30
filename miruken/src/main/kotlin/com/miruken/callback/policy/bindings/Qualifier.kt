package com.miruken.callback.policy.bindings

import com.miruken.callback.FilteringProviderFactory
import kotlin.reflect.KClass

open class Qualifier(
        val qualifierClass: KClass<out Annotation>
) : BindingConstraint {

    override fun require(metadata: BindingMetadata) =
            metadata.set(qualifierClass, null)

    override fun matches(metadata: BindingMetadata) =
            metadata.isEmpty() || metadata.has(qualifierClass)

    companion object {
        inline operator fun <reified T: Annotation> invoke() =
                Qualifier(T::class)
    }
}

object QualifierFactory : FilteringProviderFactory {
    override fun createProvider(annotation: Annotation) =
            ConstraintProvider(Qualifier(annotation.annotationClass))
}
package com.miruken.map

import com.miruken.callback.policy.BivariantPolicy
import com.miruken.callback.policy.PolicyMethodBinding
import com.miruken.callback.policy.UsePolicy
import com.miruken.runtime.getTaggedAnnotations
import kotlin.reflect.KAnnotatedElement

@Target(AnnotationTarget.FUNCTION)
@UsePolicy<MapsPolicy>(MapsPolicy::class)
annotation class Maps

object MapsPolicy : BivariantPolicy({
    key { cb: MapFrom -> cb.targetType } target { it.source } rules {
        matches (target) returns key
        matches (target, callback) returns key
        matches (callback) returns (key or unit)
    }
}) {
    override fun getKey(callback: Any): Any? =
            (callback as? MapFrom)?.sourceType?.let {
                output.getKey(callback) to it
            } ?: output.getKey(callback) to input.getKey(callback)

    override fun approve(
            callback: Any,
            binding:  PolicyMethodBinding
    ) = (callback as? MapFrom)?.let {
            val format = it.format ?: return true
            return binding.dispatcher.let {
                matches(format, it) || matches(format, it.owningClass)
            }
        } ?: false

    private fun matches(format: Any, sources: KAnnotatedElement) =
            sources.getTaggedAnnotations<UseFormatMatcher<*>>().any {
                val (annotation, match) = it
                match.any {
                    it.formatMatcherClass.objectInstance
                            ?.matches(annotation, format) == true
                }
            }
}
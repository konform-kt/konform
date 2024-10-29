package io.konform.validation.validations

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import kotlin.reflect.KClass
import kotlin.reflect.safeCast

/**
 * Can run a validation if the input is of a specific type.
 * @param clazz Class tag for the type
 * @param required If true, will give an error if the actual input is not of the type.
 * If false, will do nothing for a different type.
 * @param validation The validation to run for the type
 * @param T The type that the input must be of
 * @param ParentT The best-defined parent type of T
 */
public class IsClassValidation<T : ParentT & Any, ParentT>(
    private val clazz: KClass<T>,
    private val required: Boolean = false,
    private val validation: Validation<T>,
) : Validation<ParentT> {
    override fun validate(value: ParentT): ValidationResult<ParentT> {
        val castedValue = clazz.safeCast(value)
        if (castedValue == null) {
            return if (required) {
                val actualType = if (value == null) "null" else value::class.simpleName
                Invalid(mapOf("" to listOf("must be a '${clazz.simpleName}', was a '$actualType'")))
            } else {
                Valid(value)
            }
        }
        return validation.validate(castedValue)
    }
}

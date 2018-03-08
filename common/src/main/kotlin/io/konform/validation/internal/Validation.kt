package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Invalid
import io.konform.validation.Property
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import kotlin.reflect.KProperty1

internal class ValueValidation<T>(
    private val constraints: List<Constraint<T>>
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val errors = constraints.filter { !it.test(value) }.map { constructHint(value, it) }
        if (errors.isEmpty()) return Valid(value)
        return Invalid(mapOf(emptyList<String>() to errors))
    }

    private fun constructHint(value: T, it: Constraint<T>) =
        (listOf(value.toString()) + it.templateValues)
            .foldIndexed(it.hint) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
}

internal abstract class AbstractValidation<R>(
    private val subValidations: List<Validation<R>>
) {
    protected fun applyValidations(propertyValue: R, keyTransform: (List<String>) -> List<String>): ValidationResult<R> {
        return subValidations.fold(Valid(propertyValue)) { existingValidation: ValidationResult<R>, validation ->
            val newValidation = validation.validate(propertyValue).mapError(keyTransform)
            existingValidation.combineWith(newValidation)
        }
    }

    private fun ValidationResult<R>.mapError(keyTransform: (List<Property>) -> List<Property>): ValidationResult<R> {
        return when (this) {
            is Valid -> this
            is Invalid -> Invalid(this.errors.mapKeys { (key, _) ->
                keyTransform(key)
            })
        }
    }

    private fun ValidationResult<R>.combineWith(other: ValidationResult<R>): ValidationResult<R> {
        return when (this) {
            is Valid -> return other
            is Invalid -> when (other) {
                is Valid -> this
                is Invalid -> {
                    Invalid((this.errors.toList() + other.errors.toList())
                        .groupBy({ it.first }, { it.second })
                        .mapValues { (_, values) -> values.flatten() })
                }
            }
        }
    }
}

internal class PropertyValidation<T, R>(
    private val property: KProperty1<T, R>,
    subValidations: List<Validation<R>>
) : AbstractValidation<R>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value)
        return applyValidations(propertyValue, keyTransform = { listOf(property.name) + it }).map { value }
    }
}

internal class OptionalPropertyValidation<T, R>(
    private val property: KProperty1<T, R?>,
    subValidations: List<Validation<R>>
) : AbstractValidation<R>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value) ?: return Valid(value)
        return applyValidations(propertyValue, keyTransform = { listOf(property.name) + it }).map { value }
    }
}

internal class RequiredPropertyValidation<T, R>(
    private val property: KProperty1<T, R?>,
    subValidations: List<Validation<R>>
) : AbstractValidation<R>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value)
            ?: return Invalid<T>(mapOf(listOf(property.name) to listOf("is required")))
        return applyValidations(propertyValue, keyTransform = { listOf(property.name) + it }).map { value }
    }
}

internal class ClassValidation<T>(
    subValidations: List<Validation<T>>
) : AbstractValidation<T>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        return applyValidations(value, keyTransform = { it })
    }
}

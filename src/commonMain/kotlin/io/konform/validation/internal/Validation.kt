package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import kotlin.reflect.KProperty1

internal class OptionalValidation<C, T: Any>(
    private val validation: Validation<C, T>
) : Validation<C, T?> {
    override fun validate(context: C, value: T?): ValidationResult<T?> {
        val nonNullValue = value ?: return Valid(value)
        return validation(context, nonNullValue)
    }
}

internal class RequiredValidation<C, T: Any>(
    private val validation: Validation<C, T>
) : Validation<C, T?> {
    override fun validate(context: C, value: T?): ValidationResult<T?> {
        val nonNullValue = value
            ?: return Invalid(mapOf("" to listOf("is required")))
        return validation(context, nonNullValue)
    }
}

internal class NonNullPropertyValidation<C, T, R>(
    private val property: KProperty1<T, R>,
    private val validation: Validation<C, R>
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> {
        val propertyValue = property(value)
        return validation(context, propertyValue).mapError { ".${property.name}$it" }.map { value }
    }
}

internal class OptionalPropertyValidation<C, T, R>(
    private val property: KProperty1<T, R?>,
    private val validation: Validation<C, R>
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> {
        val propertyValue = property(value) ?: return Valid(value)
        return validation(context, propertyValue).mapError { ".${property.name}$it" }.map { value }
    }
}

internal class RequiredPropertyValidation<C, T, R>(
    private val property: KProperty1<T, R?>,
    private val validation: Validation<C, R>
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> {
        val propertyValue = property(value)
            ?: return Invalid<T>(mapOf(".${property.name}" to listOf("is required")))
        return validation(context, propertyValue).mapError { ".${property.name}${it}" }.map { value }
    }
}

internal class IterableValidation<C, T>(
    private val validation: Validation<C, T>
) : Validation<C, Iterable<T>> {
    override fun validate(context: C, value: Iterable<T>): ValidationResult<Iterable<T>> {
        return value.foldIndexed(Valid(value)) { index, result: ValidationResult<Iterable<T>>, propertyValue ->
            val propertyValidation = validation(context, propertyValue).mapError { "[$index]$it" }.map { value }
            result.combineWith(propertyValidation)
        }
    }
}

internal class ArrayValidation<C, T>(
    private val validation: Validation<C, T>
) : Validation<C, Array<T>> {
    override fun validate(context: C, value: Array<T>): ValidationResult<Array<T>> {
        return value.foldIndexed(Valid(value)) { index, result: ValidationResult<Array<T>>, propertyValue ->
            val propertyValidation = validation(context, propertyValue).mapError { "[$index]$it" }.map { value }
            result.combineWith(propertyValidation)
        }
    }
}

internal class MapValidation<C, K, V>(
    private val validation: Validation<C, Map.Entry<K, V>>
) : Validation<C, Map<K, V>> {
    override fun validate(context: C, value: Map<K, V>): ValidationResult<Map<K, V>> {
        return value.asSequence().fold(Valid(value)) { result: ValidationResult<Map<K, V>>, entry ->
            val propertyValidation = validation(context, entry).mapError { ".${entry.key.toString()}${it.removePrefix(".value")}" }.map { value }
            result.combineWith(propertyValidation)
        }
    }
}

internal class ValidationNode<C, T>(
    private val constraints: List<Constraint<C, T>>,
    private val subValidations: List<Validation<C, T>>
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> {
        val subValidationResult = applySubValidations(context, value, keyTransform = { it })
        val localValidationResult = localValidation(context, value)
        return localValidationResult.combineWith(subValidationResult)
    }

    private fun localValidation(context: C, value: T): ValidationResult<T> {
        return constraints
            .filter { !it.test(context, value) }
            .map { constructHint(value, it) }
            .let { errors ->
                if (errors.isEmpty()) {
                    Valid(value)
                } else {
                    Invalid(mapOf("" to errors))
                }
            }
    }

    private fun constructHint(value: T, it: Constraint<C, T>): String {
        val replaceValue = it.hint.replace("{value}", value.toString())
        return it.templateValues
            .foldIndexed(replaceValue) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
    }

    private fun applySubValidations(context: C, propertyValue: T, keyTransform: (String) -> String): ValidationResult<T> {
        return subValidations.fold(Valid(propertyValue)) { existingValidation: ValidationResult<T>, validation ->
            val newValidation = validation.validate(context, propertyValue).mapError(keyTransform)
            existingValidation.combineWith(newValidation)
        }
    }
}

internal fun <R> ValidationResult<R>.mapError(keyTransform: (String) -> String): ValidationResult<R> {
    return when (this) {
        is Valid -> this
        is Invalid -> Invalid(this.internalErrors.mapKeys { (key, _) ->
            keyTransform(key)
        })
    }
}

internal fun <R> ValidationResult<R>.combineWith(other: ValidationResult<R>): ValidationResult<R> {
    return when (this) {
        is Valid -> return other
        is Invalid -> when (other) {
            is Valid -> this
            is Invalid -> {
                Invalid((this.internalErrors.toList() + other.internalErrors.toList())
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, values) -> values.flatten() })
            }
        }
    }
}

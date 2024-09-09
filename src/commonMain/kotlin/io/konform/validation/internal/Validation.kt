package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult
import kotlin.reflect.KProperty1

internal class OptionalValidation<T : Any>(
    private val validation: Validation<T>,
) : Validation<T?> {
    override fun validate(value: T?): ValidationResult<T?> {
        val nonNullValue = value ?: return Valid(value)
        return validation(nonNullValue)
    }
}

internal class RequiredValidation<T : Any>(
    private val validation: Validation<T>,
) : Validation<T?> {
    override fun validate(value: T?): ValidationResult<T?> {
        val nonNullValue =
            value
                ?: return Invalid(mapOf("" to listOf("is required")))
        return validation(nonNullValue)
    }
}

internal class NonNullPropertyValidation<T, R>(
    private val property: KProperty1<T, R>,
    private val validation: Validation<R>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value)
        return validation(propertyValue).mapError { ".${property.name}$it" }.map { value }
    }
}

internal class OptionalPropertyValidation<T, R>(
    private val property: KProperty1<T, R?>,
    private val validation: Validation<R>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value) ?: return Valid(value)
        return validation(propertyValue).mapError { ".${property.name}$it" }.map { value }
    }
}

internal class RequiredPropertyValidation<T, R>(
    private val property: KProperty1<T, R?>,
    private val validation: Validation<R>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue =
            property(value)
                ?: return Invalid(mapOf(".${property.name}" to listOf("is required")))
        return validation(propertyValue).mapError { ".${property.name}$it" }.map { value }
    }
}

internal class IterableValidation<T>(
    private val validation: Validation<T>,
) : Validation<Iterable<T>> {
    override fun validate(value: Iterable<T>): ValidationResult<Iterable<T>> =
        value.foldIndexed(Valid(value)) { index, result: ValidationResult<Iterable<T>>, propertyValue ->
            val propertyValidation = validation(propertyValue).mapError { "[$index]$it" }.map { value }
            result.combineWith(propertyValidation)
        }
}

internal class ArrayValidation<T>(
    private val validation: Validation<T>,
) : Validation<Array<T>> {
    override fun validate(value: Array<T>): ValidationResult<Array<T>> =
        value.foldIndexed(Valid(value)) { index, result: ValidationResult<Array<T>>, propertyValue ->
            val propertyValidation = validation(propertyValue).mapError { "[$index]$it" }.map { value }
            result.combineWith(propertyValidation)
        }
}

internal class MapValidation<K, V>(
    private val validation: Validation<Map.Entry<K, V>>,
) : Validation<Map<K, V>> {
    override fun validate(value: Map<K, V>): ValidationResult<Map<K, V>> =
        value.asSequence().fold(Valid(value)) { result: ValidationResult<Map<K, V>>, entry ->
            val propertyValidation = validation(entry).mapError { ".${entry.key}${it.removePrefix(".value")}" }.map { value }
            result.combineWith(propertyValidation)
        }
}

internal class ValidationNode<T>(
    private val constraints: List<Constraint<T>>,
    private val subValidations: List<Validation<T>>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val subValidationResult = applySubValidations(value, keyTransform = { it })
        val localValidationResult = localValidation(value)
        return localValidationResult.combineWith(subValidationResult)
    }

    private fun localValidation(value: T): ValidationResult<T> =
        constraints
            .filter { !it.test(value) }
            .map { constructHint(value, it) }
            .let { errors ->
                if (errors.isEmpty()) {
                    Valid(value)
                } else {
                    Invalid(mapOf("" to errors))
                }
            }

    private fun constructHint(
        value: T,
        it: Constraint<T>,
    ): String {
        val replaceValue = it.hint.replace("{value}", value.toString())
        return it.templateValues
            .foldIndexed(replaceValue) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
    }

    private fun applySubValidations(
        propertyValue: T,
        keyTransform: (String) -> String,
    ): ValidationResult<T> =
        subValidations.fold(Valid(propertyValue)) { existingValidation: ValidationResult<T>, validation ->
            val newValidation = validation.validate(propertyValue).mapError(keyTransform)
            existingValidation.combineWith(newValidation)
        }
}

internal fun <R> ValidationResult<R>.mapError(keyTransform: (String) -> String): ValidationResult<R> =
    when (this) {
        is Valid -> this
        is Invalid ->
            Invalid(
                this.internalErrors.mapKeys { (key, _) ->
                    keyTransform(key)
                },
            )
    }

internal fun <R> ValidationResult<R>.combineWith(other: ValidationResult<R>): ValidationResult<R> {
    return when (this) {
        is Valid -> return other
        is Invalid ->
            when (other) {
                is Valid -> this
                is Invalid -> {
                    Invalid(
                        (this.internalErrors.toList() + other.internalErrors.toList())
                            .groupBy({ it.first }, { it.second })
                            .mapValues { (_, values) -> values.flatten() },
                    )
                }
            }
    }
}

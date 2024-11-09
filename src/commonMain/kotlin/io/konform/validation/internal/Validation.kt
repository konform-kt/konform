package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.ValidationResult
import io.konform.validation.path.ValidationPath

/** A property that is required and not null. */
internal class NonNullPropertyValidation<T, R>(
    val property: (T) -> R,
    private val name: String,
    private val validation: Validation<R>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        return
        val propertyValue = property(value)
        return validation(propertyValue).mapError { ".${name}$it" }.map { value }
    }
}

/** A property that is optional and nullable. */
internal class OptionalPropertyValidation<T, R>(
    val property: (T) -> R?,
    private val name: String,
    private val validation: Validation<R>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value) ?: return Valid(value)
        return validation(propertyValue).mapError { ".${name}$it" }.map { value }
    }
}

/** A property that is nullable, but still required. */
internal class RequiredPropertyValidation<T, R>(
    val property: (T) -> R?,
    private val name: String,
    private val validation: Validation<R>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue =
            property(value)
                ?: return Invalid(mapOf(".$name" to listOf("is required")))
        return validation(propertyValue).mapError { ".${name}$it" }.map { value }
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

internal class ConstraintsValidation<T>(
    private val path: ValidationPath,
    private val constraints: List<Constraint<T>>,
) : Validation<T> {
    override fun validate(value: T): ValidationResult<T> =
        constraints
            .filterNot { it.test(value) }
            .map { ValidationError(path, it.createHint(value)) }
            .let { errors ->
                if (errors.isEmpty()) {
                    Valid(value)
                } else {
                    Invalid(errors)
                }
            }
}

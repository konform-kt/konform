package io.konform.validation.internal

import io.konform.validation.Invalid
import io.konform.validation.Valid
import io.konform.validation.Validation
import io.konform.validation.ValidationResult

internal class MappedContextValidation<C, S, T>(
    private val validation: Validation<S, T>,
    private val map: (C) -> S,
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> =
        validation(map(context), value)
}

internal class MappedValidation<C, T, V>(
    private val validation: Validation<C, V>,
    private val name: String,
    private val mapFn: (T) -> V,
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> =
        validation(context, mapFn(value))
            .mapError { ".$name$it" }
            .map { value }
}

internal class OptionalValidation<C, T : Any>(
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

internal data class ConstraintValidation<C, T>(
    private val hint: String,
    private val templateValues: List<String>,
    private val test: (C, T) -> Boolean,
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> =
        if (test(context, value)) {
            Valid(value)
        } else {
            Invalid(mapOf("" to listOf(constructHint(value))))
        }

    private fun constructHint(value: T): String {
        val replaceValue = hint.replace("{value}", value.toString())
        return templateValues
            .foldIndexed(replaceValue) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
    }

}

internal class ValidationNode<C, T>(
    private val subValidations: List<Validation<C, T>>
) : Validation<C, T> {
    override fun validate(context: C, value: T): ValidationResult<T> =
        subValidations.fold(Valid(value)) { existingValidation: ValidationResult<T>, validation ->
            val newValidation = validation.validate(context, value).mapError(::identity)
            existingValidation.combineWith(newValidation)
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

internal fun <A> identity(a: A): A = a

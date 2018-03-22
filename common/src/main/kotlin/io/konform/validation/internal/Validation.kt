package io.konform.validation.internal

import io.konform.validation.Constraint
import io.konform.validation.Invalid
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

    private fun constructHint(value: T, it: Constraint<T>): String {
        val replaceValue = it.hint.replace("{value}", value.toString())
        return it.templateValues
            .foldIndexed(replaceValue) { index, hint, templateValue -> hint.replace("{$index}", templateValue) }
    }
}


internal abstract class AbstractPropertyValidation<R>(
    private val subValidations: List<Validation<R>>
) {
    protected fun applyValidations(propertyValue: R, keyTransform: (List<String>) -> List<String>): ValidationResult<R> {
        return subValidations.fold(Valid(propertyValue)) { existingValidation: ValidationResult<R>, validation ->
            val newValidation = validation.validate(propertyValue).mapError(keyTransform)
            existingValidation.combineWith(newValidation)
        }
    }
}

internal class NonNullPropertyValidation<T, R>(
    private val property: KProperty1<T, R>,
    subValidations: List<Validation<R>>
) : AbstractPropertyValidation<R>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value)
        return applyValidations(propertyValue, keyTransform = { listOf(property.name) + it }).map { value }
    }
}

internal class OptionalPropertyValidation<T, R>(
    private val property: KProperty1<T, R?>,
    subValidations: List<Validation<R>>
) : AbstractPropertyValidation<R>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value) ?: return Valid(value)
        return applyValidations(propertyValue, keyTransform = { listOf(property.name) + it }).map { value }
    }
}

internal class RequiredPropertyValidation<T, R>(
    private val property: KProperty1<T, R?>,
    subValidations: List<Validation<R>>
) : AbstractPropertyValidation<R>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        val propertyValue = property(value)
            ?: return Invalid<T>(mapOf(listOf(property.name) to listOf("is required")))
        return applyValidations(propertyValue, keyTransform = { listOf(property.name) + it }).map { value }
    }
}

internal class IterableValidation<T>(
    subValidations: List<Validation<T>>
) : AbstractPropertyValidation<T>(subValidations), Validation<Iterable<T>> {
    override fun validate(value: Iterable<T>): ValidationResult<Iterable<T>> {
        return value.foldIndexed(Valid(value)) { index, result: ValidationResult<Iterable<T>>, propertyValue ->
            val propertyValidation = applyValidations(propertyValue, keyTransform = { listOf(index.toString()) + it }).map { value }
            result.combineWith(propertyValidation)
        }

    }
}

internal class ArrayValidation<T>(
    subValidations: List<Validation<T>>
) : AbstractPropertyValidation<T>(subValidations), Validation<Array<T>> {
    override fun validate(value: Array<T>): ValidationResult<Array<T>> {
        return value.foldIndexed(Valid(value)) { index, result: ValidationResult<Array<T>>, propertyValue ->
            val propertyValidation = applyValidations(propertyValue, keyTransform = { listOf(index.toString()) + it }).map { value }
            result.combineWith(propertyValidation)
        }

    }
}

internal class MapValidation<K, V>(
    subValidations: List<Validation<Map.Entry<K, V>>>
) : AbstractPropertyValidation<Map.Entry<K, V>>(subValidations), Validation<Map<K, V>> {
    override fun validate(value: Map<K, V>): ValidationResult<Map<K, V>> {
        return value.asSequence().fold(Valid(value)) { result: ValidationResult<Map<K, V>>, entry ->
            val propertyValidation = applyValidations(entry, keyTransform = { listOf(entry.key.toString()) + it.drop(1) }).map { value }
            result.combineWith(propertyValidation)
        }

    }
}

internal class ClassValidation<T>(
    subValidations: List<Validation<T>>
) : AbstractPropertyValidation<T>(subValidations), Validation<T> {
    override fun validate(value: T): ValidationResult<T> {
        return applyValidations(value, keyTransform = { it })
    }
}

internal fun <R> ValidationResult<R>.mapError(keyTransform: (List<String>) -> List<String>): ValidationResult<R> {
    return when (this) {
        is Valid -> this
        is Invalid -> Invalid(this.errors.mapKeys { (key, _) ->
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
                Invalid((this.errors.toList() + other.errors.toList())
                    .groupBy({ it.first }, { it.second })
                    .mapValues { (_, values) -> values.flatten() })
            }
        }
    }
}

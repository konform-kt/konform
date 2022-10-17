package io.konform.validation.internal

import io.konform.validation.*

internal class MappedContextValidation<C, S, T, E>(
    private val validation: Validation<S, T, E>,
    private val map: (C) -> S,
) : Validation<C, T, E> {
    override fun validate(context: C, value: T): ValidationResult<E, T> =
        validation(map(context), value)
}

internal class MappedValidation<C, T, V, E>(
    private val validation: Validation<C, V, E>,
    private val name: String,
    private val mapFn: (T) -> V,
) : Validation<C, T, E> {
    override fun validate(context: C, value: T): ValidationResult<E, T> =
        validation(context, mapFn(value))
            .mapError { ".$name$it" }
            .map { value }
}

internal class OptionalValidation<C, T : Any, E>(
    private val validation: Validation<C, T, E>
) : Validation<C, T?, E> {
    override fun validate(context: C, value: T?): ValidationResult<E, T?> {
        val nonNullValue = value ?: return Valid(value)
        return validation(context, nonNullValue)
    }
}

internal class RequiredValidation<C, T: Any, E>(
    private val requiredValidation: Validation<C, T?, E>,
    private val subValidation: Validation<C, T, E>,
) : Validation<C, T?, E> {
    override fun validate(context: C, value: T?): ValidationResult<E, T?> {
        return requiredValidation.validate(context, value)
            .flatMap {
                subValidation(context, it!!)
            }
    }
}

internal class IterableValidation<C, T, E>(
    private val validation: Validation<C, T, E>
) : Validation<C, Iterable<T>, E> {
    override fun validate(context: C, value: Iterable<T>): ValidationResult<E, Iterable<T>> {
        return value.foldIndexed(Valid(value)) { index, result: ValidationResult<E, Iterable<T>>, propertyValue ->
            val propertyValidation = validation(context, propertyValue).mapError { "[$index]$it" }.map { value }
            result.combineWith(propertyValidation)
        }
    }
}

internal class ArrayValidation<C, T, E>(
    private val validation: Validation<C, T, E>
) : Validation<C, Array<T>, E> {
    override fun validate(context: C, value: Array<T>): ValidationResult<E, Array<T>> {
        return value.foldIndexed(Valid(value)) { index, result: ValidationResult<E, Array<T>>, propertyValue ->
            val propertyValidation = validation(context, propertyValue).mapError { "[$index]$it" }.map { value }
            result.combineWith(propertyValidation)
        }
    }
}

internal class MapValidation<C, K, V, E>(
    private val validation: Validation<C, Map.Entry<K, V>, E>
) : Validation<C, Map<K, V>, E> {
    override fun validate(context: C, value: Map<K, V>): ValidationResult<E, Map<K, V>> {
        return value.asSequence().fold(Valid(value)) { result: ValidationResult<E, Map<K, V>>, entry ->
            val propertyValidation = validation(context, entry).mapError { ".${entry.key.toString()}${it.removePrefix(".value")}" }.map { value }
            result.combineWith(propertyValidation)
        }
    }
}

internal data class ConstraintValidation<C, T, E>(
    private val hint: HintBuilder<C, T, E>,
    private val arguments: HintArguments,
    private val test: (C, T) -> Boolean,
) : Validation<C, T, E> {
    override fun validate(context: C, value: T): ValidationResult<E, T> =
        if (test(context, value)) {
            Valid(value)
        } else {
            Invalid(mapOf("" to listOf(context.hint(value, arguments))))
        }
}

internal class ValidationNode<C, T, E>(
    private val subValidations: List<Validation<C, T, E>>
) : Validation<C, T, E> {
    override fun validate(context: C, value: T): ValidationResult<E, T> =
        subValidations.fold(Valid(value)) { existingValidation: ValidationResult<E, T>, validation ->
            val newValidation = validation.validate(context, value).mapError(::identity)
            existingValidation.combineWith(newValidation)
        }
}

internal fun <R, E> ValidationResult<R, E>.mapError(keyTransform: (String) -> String): ValidationResult<R, E> {
    return when (this) {
        is Valid -> this
        is Invalid -> Invalid(this.internalErrors.mapKeys { (key, _) ->
            keyTransform(key)
        })
    }
}

internal fun <R, E> ValidationResult<R, E>.combineWith(other: ValidationResult<R, E>): ValidationResult<R, E> {
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

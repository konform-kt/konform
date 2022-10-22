package io.konform.validation

import kotlin.reflect.KProperty1

interface ValidationError<out E> {
    val dataPath: String
    val message: E
}

internal data class PropertyValidationError<E>(
    override val dataPath: String,
    override val message: E,
) : ValidationError<E> {
    override fun toString(): String {
        return "ValidationError(dataPath=$dataPath, message=$message)"
    }
}

interface ValidationErrors<out E> : List<ValidationError<E>>

internal object NoValidationErrors : ValidationErrors<Nothing>, List<ValidationError<Nothing>> by emptyList()
internal class DefaultValidationErrors<E>(private val errors: List<ValidationError<E>>) : ValidationErrors<E>, List<ValidationError<E>> by errors {
    override fun toString(): String {
        return errors.toString()
    }
}

sealed class ValidationResult<out E, out T> {
    abstract val errors: ValidationErrors<E>

    abstract operator fun get(vararg propertyPath: Any): List<E>?

    fun <R> map(transform: (T) -> R): ValidationResult<E, R> =
        flatMap { flatMap { Valid(transform(it)) } }
}

data class Invalid<out E>(
    internal val internalErrors: Map<String, List<E>>,
) : ValidationResult<E, Nothing>() {

    override fun get(vararg propertyPath: Any): List<E>? =
        internalErrors[propertyPath.joinToString("", transform = ::toPathSegment)]

    private fun toPathSegment(it: Any): String {
        return when (it) {
            is KProperty1<*, *> -> ".${it.name}"
            is Int -> "[$it]"
            else -> ".$it"
        }
    }

    override val errors: ValidationErrors<E> by lazy {
        DefaultValidationErrors(
            internalErrors.flatMap { (path, errors ) ->
                errors.map { PropertyValidationError(path, it) }
            }
        )
    }

    override fun toString(): String {
        return "Invalid(errors=${errors})"
    }
}

data class Valid<out E, out T>(val value: T) : ValidationResult<E, T>() {
    override fun get(vararg propertyPath: Any): List<E>? = null
    override val errors: ValidationErrors<E>
        get() = DefaultValidationErrors(emptyList())
}

inline fun <A, B, C> ValidationResult<A, B>.flatMap(f: (B) -> ValidationResult<A, C>): ValidationResult<A, C> =
    when (this) {
        is Invalid -> this
        is Valid -> f(this.value)
    }

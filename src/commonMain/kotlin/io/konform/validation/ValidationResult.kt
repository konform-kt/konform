package io.konform.validation

import kotlin.reflect.KProperty1

public interface ValidationError {
    public val dataPath: String
    public val message: String
}

internal data class PropertyValidationError(
    override val dataPath: String,
    override val message: String,
) : ValidationError {
    override fun toString(): String {
        return "ValidationError(dataPath=$dataPath, message=$message)"
    }
}

public interface ValidationErrors : List<ValidationError>

internal object NoValidationErrors : ValidationErrors, List<ValidationError> by emptyList()

internal class DefaultValidationErrors(private val errors: List<ValidationError>) : ValidationErrors, List<ValidationError> by errors {
    override fun toString(): String {
        return errors.toString()
    }
}

public sealed class ValidationResult<out T> {
    public abstract operator fun get(vararg propertyPath: Any): List<String>?

    /**  If this is a valid result, returns the result of applying the given [transform] function to the value. Otherwise, return the original error. */
    public inline fun <R> map(transform: (T) -> R): ValidationResult<R> =
        when (this) {
            is Valid -> Valid(transform(this.value))
            is Invalid -> this
        }

    public abstract val errors: ValidationErrors
}

public data class Invalid(
    internal val internalErrors: Map<String, List<String>>,
) : ValidationResult<Nothing>() {
    override fun get(vararg propertyPath: Any): List<String>? = internalErrors[propertyPath.joinToString("", transform = ::toPathSegment)]

    private fun toPathSegment(it: Any): String {
        return when (it) {
            is KProperty1<*, *> -> ".${it.name}"
            is Int -> "[$it]"
            else -> ".$it"
        }
    }

    override val errors: ValidationErrors by lazy {
        DefaultValidationErrors(
            internalErrors.flatMap { (path, errors) ->
                errors.map { PropertyValidationError(path, it) }
            },
        )
    }

    override fun toString(): String {
        return "Invalid(errors=$errors)"
    }
}

public data class Valid<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null

    override val errors: ValidationErrors
        get() = DefaultValidationErrors(emptyList())
}

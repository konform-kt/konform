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

    public abstract fun <R> map(transform: (T) -> R): ValidationResult<R>

    public abstract val errors: ValidationErrors
}

public data class Invalid<T>(
    internal val internalErrors: Map<String, List<String>>,
) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = internalErrors[propertyPath.joinToString("", transform = ::toPathSegment)]

    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Invalid(this.internalErrors)

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

    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Valid(transform(this.value))

    override val errors: ValidationErrors
        get() = DefaultValidationErrors(emptyList())
}

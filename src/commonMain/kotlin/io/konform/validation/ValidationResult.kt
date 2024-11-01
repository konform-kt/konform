package io.konform.validation

import io.konform.validation.kotlin.Path
import io.konform.validation.path.ValidationPathElement

public interface ValidationError {
    public val message: String
    public val path: List<ValidationPathElement>
    public val dataPath: String get() = ValidationPathElement.toStringPath(path)
}

internal data class PropertyValidationError(
    override val path: List<ValidationPathElement>,
    override val message: String,
) : ValidationError {
    override fun toString(): String = "ValidationError(path=$path, message=$message)"
}

public sealed class ValidationResult<out T> {
    /** Get the validation errors at a specific path. Will return null for a valid result. */
    public abstract operator fun get(vararg propertyPath: Any): List<String>?

    /**  If this is a valid result, returns the result of applying the given [transform] function to the value. Otherwise, return the original error. */
    public inline fun <R> map(transform: (T) -> R): ValidationResult<R> =
        when (this) {
            is Valid -> Valid(transform(this.value))
            is Invalid -> this
        }

    public abstract val errors: List<ValidationError>

    /**
     * Returns true if the [ValidationResult] is [Valid].
     */
    public val isValid: Boolean =
        when (this) {
            is Invalid -> false
            is Valid -> true
        }
}

public data class Invalid(
    internal val internalErrors: Map<ValidationPathElement, List<String>>,
) : ValidationResult<Nothing>() {
    override fun get(vararg propertyPath: Any): List<String>? = internalErrors[Path.toPath(*propertyPath)]

    override val errors: List<ValidationError> by lazy {
        internalErrors.flatMap { (path, errors) ->
            errors.map { PropertyValidationError(path, it) }
        }
    }

    override fun toString(): String = "Invalid(errors=$errors)"
}

public data class Valid<T>(
    val value: T,
) : ValidationResult<T>() {
    // This will not be removed as long as ValidationResult has it, but we still deprecate it to warn the user
    // that it is nonsensical to do.
    @Deprecated("It is not useful to index a valid result, it will always return null", ReplaceWith("null"))
    override fun get(vararg propertyPath: Any): List<String>? = null

    // This will not be removed as long as ValidationResult has it, but we still deprecate it to warn the user
    // that it is nonsensical to do.
    @Deprecated("It is not useful to call errors on a valid result, it will always return an empty list.", ReplaceWith("emptyList()"))
    override val errors: List<ValidationError>
        get() = emptyList()
}

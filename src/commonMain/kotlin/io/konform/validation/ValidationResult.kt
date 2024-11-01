package io.konform.validation

import io.konform.validation.kotlin.Path
import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath

public data class ValidationError(
    public val path: ValidationPath,
    public val message: String,
) {
    public val dataPath: String get() = PathSegment.toStringPath(path)
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

    /** Returns true if the [ValidationResult] is [Valid]. */
    public abstract val isValid: Boolean
}

public data class Invalid(
    private val internalErrors: List<Pair<ValidationPath>>,
) : ValidationResult<Nothing>() {
    override fun get(vararg propertyPath: Any): List<String>? = internalErrors[Path.toPath(*propertyPath)]

    override val errors: List<ValidationError> by lazy {
        internalErrors.flatMap { (path, errors) ->
            errors.map { ValidationError(path, it) }
        }
    }

    override val isValid: Boolean get() = false
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

    override val isValid: Boolean get() = true
}

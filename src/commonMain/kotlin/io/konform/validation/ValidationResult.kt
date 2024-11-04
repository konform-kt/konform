package io.konform.validation

import io.konform.validation.path.ValidationPath

public data class ValidationError(
    public val path: ValidationPath,
    public val message: String,
) {
    public val dataPath: String get() = path.pathString

    internal fun prepend(path: ValidationPath) = this.copy(path = this.path.prepend(path))
}

public sealed class ValidationResult<out T> {
    /** Get the validation errors at a specific path. Will return empty list for [Valid]. */
    public abstract operator fun get(vararg validationPath: Any): List<String>

    /**  If this is a valid result, returns the result of applying the given [transform] function to the value. Otherwise, return the original error. */
    public inline fun <R> map(transform: (T) -> R): ValidationResult<R> =
        when (this) {
            is Valid -> Valid(transform(this.value))
            is Invalid -> this
        }

    public abstract val errors: List<ValidationError>

    /** Returns true if the [ValidationResult] is [Valid]. */
    public abstract val isValid: Boolean

    internal fun <R> subValidate(
        path: ValidationPath,
        validation: Validation<R>,
        value: R,
    ): ValidationResult<T> =
        when (val result = validation.validate(value)) {
            is Valid -> this
            is Invalid -> {
                val myErrors = (this as? Invalid)?.errors ?: emptyList()
                val newErrors = myErrors + result.errors.map { it.prepend(path) }
                Invalid(newErrors)
            }
        }
}

public data class Invalid(
    override val errors: List<ValidationError>,
) : ValidationResult<Nothing>() {
    override fun get(vararg validationPath: Any): List<String> {
        val path = ValidationPath.fromAny(*validationPath)
        return errors.filter { it.path == path }.map { it.message }
    }

    override val isValid: Boolean get() = false
}

public data class Valid<T>(
    val value: T,
) : ValidationResult<T>() {
    override val isValid: Boolean get() = true

    // ValidationResult has errors so this needs to have it, but we deprecate it to warn the user
    // that it is nonsensical to do.
    @Deprecated(
        "It is not useful to index a valid result, it will always return empty list",
        ReplaceWith("emptyList()"),
    )
    override fun get(vararg validationPath: Any): List<String> = emptyList()

    @Deprecated(
        "It is not useful to call errors on a valid result, it will always return an empty list.",
        ReplaceWith("emptyList()"),
    )
    override val errors: List<ValidationError> get() = emptyList()
}

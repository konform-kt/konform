package io.konform.validation

import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath

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

    internal abstract fun prependPath(pathSegment: PathSegment): ValidationResult<T>

    internal abstract fun prependPath(path: ValidationPath): ValidationResult<T>

    internal infix operator fun plus(other: ValidationResult<@UnsafeVariance T>): ValidationResult<T> =
        when (this) {
            is Valid -> other
            is Invalid ->
                when (other) {
                    is Valid -> this
                    is Invalid -> Invalid(errors + other.errors)
                }
        }

    /** Add the result of a sub-validation to the current result. */
    internal fun mergeSub(
        currentPath: ValidationPath,
        subResult: ValidationResult<*>,
    ): ValidationResult<T> =
        when (subResult) {
            is Valid -> this
            is Invalid -> {
                val subErrors = subResult.errors.map { it.prependPath(currentPath) }
                Invalid(
                    if (this is Valid) subErrors else errors + subErrors,
                )
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

    override fun prependPath(pathSegment: PathSegment): Invalid = Invalid(errors.map { it.prependPath(pathSegment) })

    override fun prependPath(path: ValidationPath): Invalid =
        if (path.segments.isEmpty()) this else Invalid(errors.map { it.prependPath(path) })

    public companion object {
        public fun of(
            path: ValidationPath,
            message: String,
        ): Invalid = Invalid(listOf(ValidationError(path, message)))
    }
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

    override fun prependPath(pathSegment: PathSegment): ValidationResult<T> = this

    override fun prependPath(path: ValidationPath): ValidationResult<T> = this

    internal companion object {
        internal val NULL: Valid<Nothing?> = Valid(null)
    }
}

internal fun <T> List<ValidationResult<T>>.flattenNonEmpty(): ValidationResult<T> {
    require(isNotEmpty()) { "List<ValidationResult> is not allowed to be empty in flattenNonEmpty" }
    val invalids = filterIsInstance<Invalid>()
    return if (invalids.isEmpty()) {
        first() as Valid
    } else {
        invalids.flattenNotEmpty()
    }
}

internal fun List<Invalid>.flattenNotEmpty(): Invalid {
    require(isNotEmpty()) { "List<Invalid> is not allowed to be empty in flattenNonEmpty" }
    return Invalid(map { it.errors }.flatten())
}

internal fun <T> List<ValidationResult<T>>.flattenOrValid(value: T): ValidationResult<T> =
    takeIf { isNotEmpty() }
        ?.flattenNonEmpty()
        ?.takeIf { it is Invalid }
        ?: Valid(value)

internal fun <T> List<Invalid>.flattenOrValid(value: T): ValidationResult<T> = if (isNotEmpty()) flattenNonEmpty() else Valid(value)

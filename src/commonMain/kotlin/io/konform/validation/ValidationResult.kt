package io.konform.validation

import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath
import kotlin.jvm.JvmName

public sealed class ValidationResult<out T> {
    /** Get the validation errors at a specific path. Will return empty list for [Valid]. */
    @Deprecated(
        "Prefer filtering errors on the ValidationPath",
        ReplaceWith(
            "errors.filter { it.path == ValidationPath.fromAny(*validationPath) }.map { it.message }",
            "io.konform.validation.path.ValidationPath",
        ),
    )
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

    /** Merge two [ValidationResult], returning [Valid] if both are valid, and the error(s) otherwise. */
    public infix operator fun plus(other: ValidationResult<@UnsafeVariance T>): ValidationResult<T> =
        when (this) {
            is Valid -> other
            is Invalid ->
                when (other) {
                    is Valid -> this
                    is Invalid -> Invalid(errors + other.errors)
                }
        }

    internal abstract fun prependPath(pathSegment: PathSegment): ValidationResult<T>

    internal abstract fun prependPath(path: ValidationPath): ValidationResult<T>
}

public data class Invalid(
    override val errors: List<ValidationError>,
) : ValidationResult<Nothing>() {
    @Deprecated(
        "Prefer filtering errors on the ValidationPath",
        replaceWith =
            ReplaceWith(
                "errors.filter { it.path == ValidationPath.fromAny(*validationPath) }.map { it.message }",
                "io.konform.validation.path.ValidationPath",
            ),
    )
    override fun get(vararg validationPath: Any): List<String> {
        val path = ValidationPath.fromAny(*validationPath)
        return errors.filter { it.dataPath == path.dataPath }.map { it.message }
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

    @Deprecated("It is not useful to call errors on a valid result, it will always return an empty list.", ReplaceWith("emptyList()"))
    override val errors: List<ValidationError>
        get() = emptyList()

    override fun prependPath(pathSegment: PathSegment): ValidationResult<T> = this

    override fun prependPath(path: ValidationPath): ValidationResult<T> = this
}

public fun <T> List<ValidationResult<T>>.flattenNonEmpty(): ValidationResult<T> {
    require(isNotEmpty()) { "List<ValidationResult> is not allowed to be empty in flattenNonEmpty" }
    val invalids = filterIsInstance<Invalid>()
    return if (invalids.isEmpty()) {
        first() as Valid
    } else {
        invalids.flattenNotEmpty()
    }
}

public fun List<Invalid>.flattenNotEmpty(): Invalid {
    require(isNotEmpty()) { "List<Invalid> is not allowed to be empty in flattenNonEmpty" }
    return Invalid(map { it.errors }.flatten())
}

public fun <T> List<ValidationResult<T>>.flattenOrValid(value: T): ValidationResult<T> =
    takeIf { isNotEmpty() }
        ?.flattenNonEmpty()
        ?.takeIf { it is Invalid }
        ?: Valid(value)

@JvmName("flattenOrValidInvalidList")
public fun <T> List<Invalid>.flattenOrValid(value: T): ValidationResult<T> = if (isNotEmpty()) flattenNonEmpty() else Valid(value)

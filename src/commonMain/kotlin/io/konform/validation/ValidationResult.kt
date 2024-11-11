package io.konform.validation

import io.konform.validation.kotlin.Path
import kotlin.jvm.JvmName

internal data class PropertyValidationError(
    override val dataPath: String,
    override val message: String,
) : ValidationError {
    override fun toString(): String = "ValidationError(dataPath=$dataPath, message=$message)"
}

@Deprecated("Replace with directly using List<ValidationError>", ReplaceWith("List<ValidationError>"))
public interface ValidationErrors : List<ValidationError>

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
    internal val internalErrors: Map<String, List<String>>,
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
    val merged = mutableMapOf<String, List<String>>()
    for (invalid in this) {
        val added =
            invalid.internalErrors.mapValues {
                merged.getOrElse(it.key, ::emptyList) + it.value
            }
        merged += added
    }
    return Invalid(merged)
}

internal fun <T> List<ValidationResult<T>>.flattenOrValid(value: T): ValidationResult<T> =
    takeIf { isNotEmpty() }
        ?.flattenNonEmpty()
        ?.takeIf { it is Invalid }
        ?: Valid(value)

@JvmName("flattenOrValidInvalidList")
internal fun <T> List<Invalid>.flattenOrValid(value: T): ValidationResult<T> = if (isNotEmpty()) flattenNonEmpty() else Valid(value)

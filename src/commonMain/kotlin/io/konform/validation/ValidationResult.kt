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

    override val errors: List<ValidationError> by lazy {
        internalErrors.flatMap { (path, errors) ->
            errors.map { PropertyValidationError(path, it) }
        }
    }

    override fun toString(): String {
        return "Invalid(errors=$errors)"
    }
}

public data class Valid<T>(val value: T) : ValidationResult<T>() {
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

public val <T> ValidationResult<T>.isValid: Boolean
    get() =
        when (this) {
            is Invalid -> false
            is Valid -> true
        }

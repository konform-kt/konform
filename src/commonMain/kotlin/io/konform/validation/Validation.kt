package io.konform.validation

import io.konform.validation.internal.CombinedValidations
import io.konform.validation.internal.EmptyValidation

public interface Validation<in T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> = ValidationBuilder.buildWithNew(init)
    }

    public fun validate(value: T): ValidationResult<@UnsafeVariance T>

    public operator fun invoke(value: T): ValidationResult<@UnsafeVariance T> = validate(value)
}

/** Combine a [List] of [Validation]s into a single one that returns all validation errors. */
public fun <T> List<Validation<T>>.flatten(): Validation<T> =
    when (size) {
        0 -> EmptyValidation
        1 -> first()
        else -> CombinedValidations(this)
    }

/**
 * @param hint for a failed validation. "{value}" will be replaced by the toString-ed value that is being validated
 * @param test the predicate that must be satisfied
 */
public class Constraint<in R> internal constructor(
    public val hint: String,
    public val templateValues: List<String> = emptyList(),
    public val test: (R) -> Boolean,
    // TODO: Add customizable Path paramater settable with a path() method
) {
    internal fun createHint(value: R): String {
        // Avoid toString()ing the value unless its needed
        val withValue =
            if (hint.contains(VALUE_IN_HINT)) {
                hint.replace(VALUE_IN_HINT, value.toString())
            } else {
                hint
            }

        return templateValues.foldIndexed(withValue) { index, hint, templateValue ->
            hint.replace("{$index}", templateValue)
        }
    }

    public companion object {
        public const val VALUE_IN_HINT: String = "{value}"
    }
}

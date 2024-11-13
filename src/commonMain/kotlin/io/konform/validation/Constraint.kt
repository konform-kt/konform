package io.konform.validation

import io.konform.validation.path.ValidationPath

/**
 * @param hint for a failed validation. "{value}" will be replaced by the toString-ed value that is being validated
 * @param path [ValidationPath] for this constraint, will be reported in [ValidationError]
 * @param userContext Optional context set by the user to add more information to the validation, e.g. severity
 * @param test the predicate that must be satisfied
 * @see [ValidationBuilder.hint]
 * @see [ValidationBuilder.path]
 * @see [ValidationBuilder.userContext]
 */
public data class Constraint<in R> public constructor(
    public val hint: String,
    public val path: ValidationPath = ValidationPath.EMPTY,
    public val userContext: Any? = null,
    @Deprecated("Put template parameters directly into the hint.")
    public val templateValues: List<String> = emptyList(),
    public val test: (R) -> Boolean,
) {
    override fun toString(): String = "Constraint(\"$hint\")"

    internal fun createHint(value: R): String {
        // Avoid toString()ing the value unless its needed
        val withValue =
            if (hint.contains(VALUE_IN_HINT)) {
                hint.replace(VALUE_IN_HINT, value.toString())
            } else {
                hint
            }

        @Suppress("DEPRECATION")
        val hintWithTemplateValues =
            templateValues.foldIndexed(withValue) { index, hint, templateValue ->
                hint.replace("{$index}", templateValue)
            }

        return hintWithTemplateValues
    }

    public companion object {
        public const val VALUE_IN_HINT: String = "{value}"
    }
}

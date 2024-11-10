package io.konform.validation

/**
 * @param hint for a failed validation. "{value}" will be replaced by the toString-ed value that is being validated
 * @param test the predicate that must be satisfied
 */
public class Constraint<in R> internal constructor(
    public val hint: String,
    @Deprecated("Put template parameters directly into the hint.")
    public val templateValues: List<String> = emptyList(),
    public val test: (R) -> Boolean,
    // TODO: Add customizable Path parameter settable with a path() method
) {
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

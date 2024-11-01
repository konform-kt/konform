package io.konform.validation

public interface Validation<in T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> = ValidationBuilder.buildWithNew(init)
    }

    public fun validate(value: T): ValidationResult<@UnsafeVariance T>

    public operator fun invoke(value: T): ValidationResult<@UnsafeVariance T> = validate(value)
}

public class Constraint<in R> internal constructor(
    public val hint: String,
    public val templateValues: List<String>,
    public val test: (R) -> Boolean,
)

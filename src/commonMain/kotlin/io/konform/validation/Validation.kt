package io.konform.validation

import io.konform.validation.internal.ValidationBuilderImpl

public interface Validation<T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = ValidationBuilderImpl<T>()
            return builder.apply(init).build()
        }
    }

    public fun validate(value: T): ValidationResult<T>

    public operator fun invoke(value: T): ValidationResult<T> = validate(value)
}

public class Constraint<R> internal constructor(
    public val hint: String,
    public val templateValues: List<String>,
    public val test: (R) -> Boolean,
)

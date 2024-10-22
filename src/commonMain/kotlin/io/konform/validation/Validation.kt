package io.konform.validation

public interface Validation<T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = ValidationBuilder<T>()
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

package io.konform.validation

public interface Validation<out T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = ValidationBuilder<T>()
            return builder.apply(init).build()
        }
    }

    public fun validate(value: @UnsafeVariance T): ValidationResult<T>

    public operator fun invoke(value: @UnsafeVariance T): ValidationResult<T> = validate(value)
}

public class Constraint<out R> internal constructor(
    public val hint: String,
    public val templateValues: List<String>,
    public val test: (@UnsafeVariance R) -> Boolean,
)

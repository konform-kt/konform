package io.konform.validation

import io.konform.validation.internal.ValidationBuilderImpl

public interface Validation<T> {
    public companion object {
        public operator fun <T> invoke(init: ValidationBuilder<T>.(Context<T>) -> Unit): Validation<T> {
            val context = Context<T>()
            val builder = ValidationBuilderImpl<T>()
            init(builder, context)
            val validation = builder.build()
            return object : Validation<T> by validation {
                override fun validate(value: T): ValidationResult<T> {
                    context.subject = value
                    return validation.validate(value)
                }

                override fun invoke(value: T): ValidationResult<T> {
                    return validate(value)
                }
            }
        }
    }

    public fun validate(value: T): ValidationResult<T>

    public operator fun invoke(value: T): ValidationResult<T> = validate(value)

    public class Context<T> {
        private var _subjectHolder: SubjectHolder<T>? = null

        public var subject: T
            get() {
                return when (val subjectHolder = _subjectHolder) {
                    null -> throw IllegalStateException("Subject not initialized")
                    else -> subjectHolder.value
                }
            }
            set(value) {
                _subjectHolder = SubjectHolder(value)
            }

        public operator fun component1(): T = subject

        private data class SubjectHolder<T>(val value: T)
    }
}

public class Constraint<R> internal constructor(
    public val hint: String,
    public val templateValues: List<String>,
    public val test: (R) -> Boolean,
)

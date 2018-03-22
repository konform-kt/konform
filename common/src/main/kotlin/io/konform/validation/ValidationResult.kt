package io.konform.validation

import kotlin.reflect.KProperty1

sealed class ValidationResult<T> {
    abstract operator fun get(vararg propertyPath: Any): List<String>?
    abstract fun <R> map(transform: (T) -> R): ValidationResult<R>
}

data class Invalid<T>(
    internal val errors: Map<List<String>, List<String>>) : ValidationResult<T>() {

    override fun get(vararg propertyPath: Any): List<String>? =
        errors[propertyPath.map(::toPathSegment)]
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Invalid(this.errors)

    private fun toPathSegment(it: Any): String {
        return when (it) {
            is KProperty1<*, *> -> it.name
            else -> it.toString()
        }
    }
}

data class Valid<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: Any): List<String>? = null
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Valid(transform(this.value))
}

package io.konform.validation

import kotlin.reflect.KProperty1

// TODO: Should be KProperty<*, *> but this is not possible due to a current bug in KotlinJS
// https://youtrack.jetbrains.com/issue/KT-15101
typealias Property = String

sealed class ValidationResult<T> {
    abstract operator fun get(vararg propertyPath: KProperty1<*, *>): List<String>?
    abstract fun <R> map(transform: (T) -> R): ValidationResult<R>
}

data class Invalid<T>(internal val errors: Map<List<Property>, List<String>>) : ValidationResult<T>() {
    override fun get(vararg propertyPath: KProperty1<*, *>): List<String>? = errors[propertyPath.map { it.name }]
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Invalid(this.errors)
}

data class Valid<T>(val value: T) : ValidationResult<T>() {
    override fun get(vararg propertyPath: KProperty1<*, *>): List<String>? = null
    override fun <R> map(transform: (T) -> R): ValidationResult<R> = Valid(transform(this.value))
}

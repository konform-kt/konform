package io.konform.validation

import kotlin.jvm.JvmName
import kotlin.reflect.KProperty1

abstract class ValidationBuilder<T> {
    abstract fun build(): Validation<T>
    abstract fun addConstraint(errorMessage: String, vararg templateValues: String, test: (T) -> Boolean): Constraint<T>
    abstract infix fun Constraint<T>.hint(hint: String): Constraint<T>
    abstract operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit)
    internal abstract fun <R> onEachIterable(prop: KProperty1<T, Iterable<R>>, init: ValidationBuilder<R>.() -> Unit)
    @JvmName("onEachIterable")
    infix fun <R> KProperty1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) = onEachIterable(this, init)
    internal abstract fun <R> onEachArray(prop: KProperty1<T, Array<R>>, init: ValidationBuilder<R>.() -> Unit)
    @JvmName("onEachArray")
    infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) = onEachArray(this, init)
    internal abstract fun <K, V> onEachMap(prop: KProperty1<T, Map<K, V>>, init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit)
    @JvmName("onEachMap")
    infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit) = onEachMap(this, init)
    abstract infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit)
    abstract infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit)
    abstract fun run(validation: Validation<T>)
    abstract val <R> KProperty1<T, R>.has: ValidationBuilder<R>
}

package io.konform.validation

import kotlin.reflect.KProperty1

actual abstract class ValidationBuilder<T> {
    actual abstract fun build(): Validation<T>
    actual abstract fun addConstraint(errorMessage: String, vararg templateValues: String, test: (T) -> Boolean): Constraint<T>
    actual abstract infix fun Constraint<T>.hint(hint: String): Constraint<T>
    actual abstract operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<R>.() -> Unit)

    internal actual abstract fun <R> onEachIterable(prop: KProperty1<T, Iterable<R>>, init: ValidationBuilder<R>.() -> Unit)
    @JvmName("onEachArray")
    actual infix fun <R> KProperty1<T, Iterable<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) {
        onEachIterable(this, init)
    }

    internal actual abstract fun <R> onEachArray(prop: KProperty1<T, Array<R>>, init: ValidationBuilder<R>.() -> Unit)
    @JvmName("onEachIterable")
    actual infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<R>.() -> Unit) {
        onEachArray(this, init)
    }

    internal actual abstract fun <K, V> onEachMap(prop: KProperty1<T, Map<K, V>>, init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit)
    @JvmName("onEachMap")
    actual infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<Map.Entry<K, V>>.() -> Unit) {
        onEachMap(this, init)
    }

    actual abstract infix fun <R> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<R>.() -> Unit)
    actual abstract infix fun <R> KProperty1<T, R?>.required(init: ValidationBuilder<R>.() -> Unit)
    actual abstract val <R> KProperty1<T, R>.has: ValidationBuilder<R>

}

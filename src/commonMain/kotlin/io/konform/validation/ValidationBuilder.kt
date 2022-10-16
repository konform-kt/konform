package io.konform.validation

import io.konform.validation.internal.*
import kotlin.jvm.JvmName
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

@DslMarker
private annotation class ValidationScope

@ValidationScope
abstract class ValidationBuilder<C, T> : ComposableBuilder<C, T> {
    abstract override fun build(): Validation<C, T>

    abstract fun addConstraint(hint: String, vararg values: String, test: C.(T) -> Boolean): ConstraintBuilder

    abstract operator fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<C, R>.() -> Unit)
    abstract operator fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<C, R>.() -> Unit)

    internal abstract fun <R> onEachIterable(name: String, mapFn: (T) -> Iterable<R>, init: ValidationBuilder<C, R>.() -> Unit)
    @JvmName("onEachIterable")
    infix fun <R> KProperty1<T, Iterable<R>>.onEach(init: ValidationBuilder<C, R>.() -> Unit) = onEachIterable(this.name, this, init)
    @JvmName("onEachIterable")
    infix fun <R> KFunction1<T, Iterable<R>>.onEach(init: ValidationBuilder<C, R>.() -> Unit) = onEachIterable(this.name, this, init)

    internal abstract fun <R> onEachArray(name: String, mapFn: (T) -> Array<R>, init: ValidationBuilder<C, R>.() -> Unit)
    @JvmName("onEachArray")
    infix fun <R> KProperty1<T, Array<R>>.onEach(init: ValidationBuilder<C, R>.() -> Unit) = onEachArray(this.name, this, init)
    @JvmName("onEachArray")
    infix fun <R> KFunction1<T, Array<R>>.onEach(init: ValidationBuilder<C, R>.() -> Unit) = onEachArray(this.name, this, init)

    internal abstract fun <K, V> onEachMap(name: String, mapFn: (T) -> Map<K, V>, init: ValidationBuilder<C, Map.Entry<K, V>>.() -> Unit)
    @JvmName("onEachMap")
    infix fun <K, V> KProperty1<T, Map<K, V>>.onEach(init: ValidationBuilder<C, Map.Entry<K, V>>.() -> Unit) = onEachMap(this.name, this, init)
    @JvmName("onEachMap")
    infix fun <K, V> KFunction1<T, Map<K, V>>.onEach(init: ValidationBuilder<C, Map.Entry<K, V>>.() -> Unit) = onEachMap(this.name, this, init)

    internal abstract fun <R : Any> ifPresent(name: String, mapFn: (T) -> R?, init: ValidationBuilder<C, R>.() -> Unit)
    infix fun <R : Any> KProperty1<T, R?>.ifPresent(init: ValidationBuilder<C, R>.() -> Unit) = ifPresent(this.name, this, init)
    infix fun <R : Any> KFunction1<T, R?>.ifPresent(init: ValidationBuilder<C, R>.() -> Unit) = ifPresent(this.name, this, init)

    internal abstract fun <R : Any> required(name: String, mapFn: (T) -> R?, init: ValidationBuilder<C, R>.() -> Unit)
    infix fun <R : Any> KProperty1<T, R?>.required(init: ValidationBuilder<C, R>.() -> Unit) = required(this.name, this, init)
    infix fun <R : Any> KFunction1<T, R?>.required(init: ValidationBuilder<C, R>.() -> Unit) = required(this.name, this, init)

    abstract fun <S> run(validation: Validation<S, T>, map: (C) -> S)
    fun run(validation: Validation<C, T>) = run(validation, ::identity)

    abstract val <R> KProperty1<T, R>.has: ValidationBuilder<C, R>
}

interface ConstraintBuilder {
    infix fun hint(hint: String) : ConstraintBuilder
}

fun <C, T : Any> ValidationBuilder<C, T?>.ifPresent(init: ValidationBuilder<C, T>.() -> Unit) {
    val builder = ValidationNodeBuilder<C, T>()
    init(builder)
    run(OptionalValidation(builder.build()))
}

fun <C, T : Any> ValidationBuilder<C, T?>.required(init: ValidationBuilder<C, T>.() -> Unit) {
    val builder = ValidationNodeBuilder<C, T>()
    init(builder)
    run(RequiredValidation(builder.build()))
}

@JvmName("onEachIterable")
fun <C, S, T : Iterable<S>> ValidationBuilder<C, T>.onEach(init: ValidationBuilder<C, S>.() -> Unit) {
    val builder = ValidationNodeBuilder<C, S>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(IterableValidation(builder.build()) as Validation<C, T>)
}

@JvmName("onEachArray")
fun <C, T> ValidationBuilder<C, Array<T>>.onEach(init: ValidationBuilder<C, T>.() -> Unit) {
    val builder = ValidationNodeBuilder<C, T>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(ArrayValidation(builder.build()) as Validation<C, Array<T>>)
}

@JvmName("onEachMap")
fun <C, K, V, T : Map<K, V>> ValidationBuilder<C, T>.onEach(init: ValidationBuilder<C, Map.Entry<K, V>>.() -> Unit) {
    val builder = ValidationNodeBuilder<C, Map.Entry<K, V>>()
    init(builder)
    @Suppress("UNCHECKED_CAST")
    run(MapValidation(builder.build()) as Validation<C, T>)
}

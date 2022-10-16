package io.konform.validation.internal

import io.konform.validation.ConstraintBuilder
import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import kotlin.collections.Map.Entry
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

internal class ValidationNodeBuilder<C, T> : ValidationBuilder<C, T>() {
    private val subBuilders = mutableListOf<ComposableBuilder<C, T>>()

    override fun build(): Validation<C, T> =
        ValidationNode(subBuilders.map { it.build() })

    override fun addConstraint(hint: String, vararg values: String, test: (C, T) -> Boolean): ConstraintBuilder =
        ConstraintValidationBuilder(hint, values.toList(), test).also { add(it) }

    override fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<C, R>.() -> Unit) =
        add(MappedValidationBuilder(createBuilder(init), this.name,this))

    override fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<C, R>.() -> Unit) =
        add(MappedValidationBuilder(createBuilder(init), this.name,this))

    override fun <R> onEachIterable(name: String, mapFn: (T) -> Iterable<R>, init: ValidationBuilder<C, R>.() -> Unit) =
        add(IterableValidationBuilder(createBuilder(init), mapFn, name))

    override fun <R> onEachArray(name: String, mapFn: (T) -> Array<R>, init: ValidationBuilder<C, R>.() -> Unit) =
        add(ArrayValidationBuilder(createBuilder(init), mapFn, name))

    override fun <K, V> onEachMap(name: String, mapFn: (T) -> Map<K, V>, init: ValidationBuilder<C, Entry<K, V>>.() -> Unit) =
        add(MapValidationBuilder(createBuilder(init), mapFn, name))

    override fun <R : Any> ifPresent(name: String, mapFn: (T) -> R?, init: ValidationBuilder<C, R>.() -> Unit) =
        add(OptionalValidationBuilder(createBuilder(init),mapFn, name))

    override fun <R : Any> required(name: String, mapFn: (T) -> R?, init: ValidationBuilder<C, R>.() -> Unit) =
        add(RequiredValidationBuilder(createBuilder(init), mapFn, name))

    override fun <S> run(validation: Validation<S, T>, map: (C) -> S) =
        add(PrebuildValidationBuilder(validation, map))

    override val <R> KProperty1<T, R>.has: ValidationBuilder<C, R>
        get() = ValidationNodeBuilder<C, R>()
            .also { add(MappedValidationBuilder(it, this.name,this)) }

    private fun <D, S> createBuilder(init: ValidationBuilder<D, S>.() -> Unit) =
        ValidationNodeBuilder<D, S>().also(init)

    private fun add(builder: ComposableBuilder<C, T>) {
        subBuilders.add(builder)
    }
}

internal interface ComposableBuilder<C, T> {
    fun build(): Validation<C, T>
}

internal class MappedValidationBuilder<C, T, V>(
    private val subBuilder: ValidationBuilder<C, V>,
    private val name: String,
    private val mapFn: (T) -> V,
) : ComposableBuilder<C, T> {
    override fun build(): Validation<C, T> = MappedValidation(subBuilder.build(), name, mapFn)
}

internal class IterableValidationBuilder<C, T, V>(
    private val subBuilder: ValidationBuilder<C, V>,
    private val mapFn: (T) -> Iterable<V>,
    private val name: String,
) : ComposableBuilder<C, T> {
    override fun build(): Validation<C, T> = MappedValidation(IterableValidation(subBuilder.build()), name, mapFn)
}

internal class ArrayValidationBuilder<C, T, V>(
    private val subBuilder: ValidationBuilder<C, V>,
    private val mapFn: (T) -> Array<V>,
    private val name: String,
) : ComposableBuilder<C, T> {
    override fun build(): Validation<C, T> = MappedValidation(ArrayValidation(subBuilder.build()), name, mapFn)
}

internal class MapValidationBuilder<C, T, K, V>(
    private val subBuilder: ValidationBuilder<C, Entry<K, V>>,
    private val mapFn: (T) -> Map<K, V>,
    private val name: String,
) : ComposableBuilder<C, T> {
    override fun build(): Validation<C, T> = MappedValidation(MapValidation(subBuilder.build()), name, mapFn)
}

internal class OptionalValidationBuilder<C, T, V: Any>(
    private val subBuilder: ValidationBuilder<C, V>,
    private val mapFn: (T) -> V?,
    private val name: String,
) : ComposableBuilder<C, T> {
    override fun build(): Validation<C, T> = MappedValidation(OptionalValidation(subBuilder.build()), name, mapFn)
}

internal class RequiredValidationBuilder<C, T, V: Any>(
    private val subBuilder: ValidationBuilder<C, V>,
    private val mapFn: (T) -> V?,
    private val name: String,
) : ComposableBuilder<C, T> {
    override fun build(): Validation<C, T> = MappedValidation(RequiredValidation(subBuilder.build()), name, mapFn)
}

internal class PrebuildValidationBuilder<C, T, S>(
    private val validation: Validation<S, T>,
    private val mapFn: (C) -> S,
) : ComposableBuilder<C, T> {
    override fun build(): Validation<C, T> = MappedContextValidation(validation, mapFn)
}

internal class ConstraintValidationBuilder<C, T>(
    private var hint: String,
    private val templateValues: List<String>,
    private val test: (C, T) -> Boolean,
) : ComposableBuilder<C, T>, ConstraintBuilder {
    override fun build(): Validation<C, T> = ConstraintValidation(hint, templateValues, test)
    override infix fun hint(hint: String): ConstraintValidationBuilder<C, T> {
        this.hint = hint
        return this
    }
}

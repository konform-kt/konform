package io.konform.validation.internal

import io.konform.validation.*
import kotlin.collections.Map.Entry
import kotlin.reflect.KFunction1
import kotlin.reflect.KProperty1

internal class ValidationNodeBuilder<C, T, E>(
    override val requiredError: E,
) : ValidationBuilder<C, T, E>() {
    private val subBuilders = mutableListOf<ComposableBuilder<C, T, E>>()

    override fun build(): Validation<C, T, E> =
        ValidationNode(subBuilders.map { it.build() })

    override fun addConstraint(hint: HintBuilder<C, T, E>, vararg values: Any, test: (C, T) -> Boolean): ConstraintBuilder<C, T, E> =
        ConstraintValidationBuilder(hint, values.toList(), test).also { add(it) }

    override fun <R> KProperty1<T, R>.invoke(init: ValidationBuilder<C, R, E>.() -> Unit) =
        add(MappedValidationBuilder(createBuilder(init), this.name,this))

    override fun <R> KFunction1<T, R>.invoke(init: ValidationBuilder<C, R, E>.() -> Unit) =
        add(MappedValidationBuilder(createBuilder(init), this.name,this))

    override fun <R> onEachIterable(name: String, mapFn: (T) -> Iterable<R>, init: ValidationBuilder<C, R, E>.() -> Unit) =
        add(IterableValidationBuilder(createBuilder(init), mapFn, name))

    override fun <R> onEachArray(name: String, mapFn: (T) -> Array<R>, init: ValidationBuilder<C, R, E>.() -> Unit) =
        add(ArrayValidationBuilder(createBuilder(init), mapFn, name))

    override fun <K, V> onEachMap(name: String, mapFn: (T) -> Map<K, V>, init: ValidationBuilder<C, Entry<K, V>, E>.() -> Unit) =
        add(MapValidationBuilder(createBuilder(init), mapFn, name))

    override fun <R : Any> ifPresent(name: String, mapFn: (T) -> R?, init: ValidationBuilder<C, R, E>.() -> Unit) =
        add(OptionalValidationBuilder(createBuilder(init),mapFn, name))

    override fun <R : Any> required(name: String, hint: HintBuilder<C, R?, E>, mapFn: (T) -> R?, init: ValidationBuilder<C, R, E>.() -> Unit): ConstraintBuilder<C, R?, E> =
        RequiredValidationBuilder(hint, createBuilder(init), mapFn, name)
            .also { add(it) }
            .requiredConstraintBuilder

    override fun <S> run(validation: Validation<S, T, E>, map: (C) -> S) =
        add(PrebuildValidationBuilder(validation, map))

    override val <R> KProperty1<T, R>.has: ValidationBuilder<C, R, E>
        get() = ValidationNodeBuilder<C, R, E>(requiredError)
            .also { add(MappedValidationBuilder(it, this.name,this)) }

    private fun <D, S> createBuilder(init: ValidationBuilder<D, S, E>.() -> Unit) =
        ValidationNodeBuilder<D, S, E>(requiredError).also(init)

    private fun add(builder: ComposableBuilder<C, T, E>) {
        subBuilders.add(builder)
    }
}

internal interface ComposableBuilder<C, T, E> {
    fun build(): Validation<C, T, E>
}

internal class MappedValidationBuilder<C, T, V, E>(
    private val subBuilder: ValidationBuilder<C, V, E>,
    private val name: String,
    private val mapFn: (T) -> V,
) : ComposableBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = MappedValidation(subBuilder.build(), name, mapFn)
}

internal class IterableValidationBuilder<C, T, V, E>(
    private val subBuilder: ValidationBuilder<C, V, E>,
    private val mapFn: (T) -> Iterable<V>,
    private val name: String,
) : ComposableBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = MappedValidation(IterableValidation(subBuilder.build()), name, mapFn)
}

internal class ArrayValidationBuilder<C, T, V, E>(
    private val subBuilder: ValidationBuilder<C, V, E>,
    private val mapFn: (T) -> Array<V>,
    private val name: String,
) : ComposableBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = MappedValidation(ArrayValidation(subBuilder.build()), name, mapFn)
}

internal class MapValidationBuilder<C, T, K, V, E>(
    private val subBuilder: ValidationBuilder<C, Entry<K, V>, E>,
    private val mapFn: (T) -> Map<K, V>,
    private val name: String,
) : ComposableBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = MappedValidation(MapValidation(subBuilder.build()), name, mapFn)
}

internal class OptionalValidationBuilder<C, T, V: Any, E>(
    private val subBuilder: ValidationBuilder<C, V, E>,
    private val mapFn: (T) -> V?,
    private val name: String,
) : ComposableBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = MappedValidation(OptionalValidation(subBuilder.build()), name, mapFn)
}

internal class RequiredValidationBuilder<C, T, V: Any, E>(
    hint: HintBuilder<C, V?, E>,
    private val subBuilder: ValidationBuilder<C, V, E>,
    private val mapFn: (T) -> V?,
    private val name: String,
) : ComposableBuilder<C, T, E> {
    val requiredConstraintBuilder: ConstraintValidationBuilder<C, V?, E> =
        ConstraintValidationBuilder(hint, emptyList()) { _, value ->
            value != null
        }

    override fun build(): Validation<C, T, E> =
        MappedValidation(
            RequiredValidation(
                requiredConstraintBuilder.build(),
                subBuilder.build(),
            ),
            name,
            mapFn,
        )
}

internal class PrebuildValidationBuilder<C, T, S, E>(
    private val validation: Validation<S, T, E>,
    private val mapFn: (C) -> S,
) : ComposableBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = MappedContextValidation(validation, mapFn)
}

internal class ConstraintValidationBuilder<C, T, E>(
    private var hint: HintBuilder<C, T, E>,
    private val arguments: HintArguments,
    private val test: (C, T) -> Boolean,
) : ComposableBuilder<C, T, E>, ConstraintBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = ConstraintValidation(hint, arguments, test)
    override infix fun hint(hint: HintBuilder<C, T, E>): ConstraintValidationBuilder<C, T, E> {
        this.hint = hint
        return this
    }
}

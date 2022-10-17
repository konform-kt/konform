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
        add(MappedValidationBuilder(IterableValidationBuilder(createBuilder(init)), name, mapFn))

    override fun <R> onEachArray(name: String, mapFn: (T) -> Array<R>, init: ValidationBuilder<C, R, E>.() -> Unit) =
        add(MappedValidationBuilder(ArrayValidationBuilder(createBuilder(init)), name, mapFn))

    override fun <K, V> onEachMap(name: String, mapFn: (T) -> Map<K, V>, init: ValidationBuilder<C, Entry<K, V>, E>.() -> Unit) =
        add(MappedValidationBuilder(MapValidationBuilder(createBuilder(init)), name, mapFn))

    override fun <R : Any> ifPresent(name: String, mapFn: (T) -> R?, init: ValidationBuilder<C, R, E>.() -> Unit) =
        add(MappedValidationBuilder(OptionalValidationBuilder(createBuilder(init)), name, mapFn))

    override fun <R : Any> required(name: String, hint: HintBuilder<C, R?, E>, mapFn: (T) -> R?, init: ValidationBuilder<C, R, E>.() -> Unit): ConstraintBuilder<C, R?, E> =
        RequiredValidationBuilder(hint, createBuilder(init))
            .also { add(MappedValidationBuilder(it, name, mapFn)) }
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
    private val subBuilder: ComposableBuilder<C, V, E>,
    private val name: String,
    private val mapFn: (T) -> V,
) : ComposableBuilder<C, T, E> {
    override fun build(): Validation<C, T, E> = MappedValidation(subBuilder.build(), name, mapFn)
}

internal class IterableValidationBuilder<C, T, E>(
    private val subBuilder: ComposableBuilder<C, T, E>,
) : ComposableBuilder<C, Iterable<T>, E> {
    override fun build(): Validation<C, Iterable<T>, E> = IterableValidation(subBuilder.build())
}

internal class ArrayValidationBuilder<C, T, E>(
    private val subBuilder: ComposableBuilder<C, T, E>,
) : ComposableBuilder<C, Array<T>, E> {
    override fun build(): Validation<C, Array<T>, E> = ArrayValidation(subBuilder.build())
}

internal class MapValidationBuilder<C, K, V, E>(
    private val subBuilder: ComposableBuilder<C, Entry<K, V>, E>,
) : ComposableBuilder<C, Map<K, V>, E> {
    override fun build(): Validation<C, Map<K, V>, E> = MapValidation(subBuilder.build())
}

internal class OptionalValidationBuilder<C, T : Any, E>(
    private val subBuilder: ComposableBuilder<C, T, E>,
) : ComposableBuilder<C, T?, E> {
    override fun build(): Validation<C, T?, E> = OptionalValidation(subBuilder.build())
}

internal class RequiredValidationBuilder<C, T : Any, E>(
    hint: HintBuilder<C, T?, E>,
    private val subBuilder: ComposableBuilder<C, T, E>,
) : ComposableBuilder<C, T?, E> {
    val requiredConstraintBuilder: ConstraintValidationBuilder<C, T?, E> =
        ConstraintValidationBuilder(hint, emptyList()) { _, value -> value != null }
    override fun build(): Validation<C, T?, E> =
        RequiredValidation(
            requiredConstraintBuilder.build(),
            subBuilder.build(),
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

package io.konform.validation.builder

import io.konform.validation.Validation
import io.konform.validation.internal.ArrayValidation
import io.konform.validation.internal.IterableValidation
import io.konform.validation.internal.MapValidation

/**
 * The key of a property which we want to validate on
 * @param T The type under validation
 * */
internal interface PropKey<T> {
    /** Combine the validation of the property into the validation of the outer type. */
    fun build(validation: Validation<*>): Validation<T>
}

/**
 * A validation on a single property
 */
internal data class SingleValuePropKey<T, R>(
    val property: (T) -> R,
    val name: String,
    val modifier: PropModifier,
) : PropKey<T> {
    @Suppress("UNCHECKED_CAST")
    override fun build(validation: Validation<*>): Validation<T> = modifier.buildValidation(property, name, validation as Validation<R>)
}

internal data class IterablePropKey<T, R>(
    val property: (T) -> Iterable<R>,
    val name: String,
    val modifier: PropModifier,
) : PropKey<T> {
    @Suppress("UNCHECKED_CAST")
    override fun build(validation: Validation<*>): Validation<T> =
        modifier.buildValidation(property, name, IterableValidation(validation as Validation<R>))
}

internal data class ArrayPropKey<T, R>(
    val property: (T) -> Array<R>,
    val name: String,
    val modifier: PropModifier,
) : PropKey<T> {
    @Suppress("UNCHECKED_CAST")
    override fun build(validation: Validation<*>): Validation<T> =
        modifier.buildValidation(property, name, ArrayValidation(validation as Validation<R>))
}

internal data class MapPropKey<T, K, V>(
    val property: (T) -> Map<K, V>,
    val name: String,
    val modifier: PropModifier,
) : PropKey<T> {
    @Suppress("UNCHECKED_CAST")
    override fun build(validation: Validation<*>): Validation<T> =
        modifier.buildValidation(property, name, MapValidation(validation as Validation<Map.Entry<K, V>>))
}

package io.konform.validation.path

import io.konform.validation.platform.callableEquals
import kotlin.reflect.KProperty1

/**
 * Represents a path through a property.
 *
 * Example:
 * ```
 * data class Person(val name: String)
 * val validation = Validation<Person> {
 *   Person::name {
 *     notBlank()
 *   }
 * }
 * val result = validation.validate(Person("")) as Invalid
 * result.errors[0] = ValidationError(ValidationPath(Prop(Person::name)), "must not be blank")
 * ```
 *
 * Note: equality differs between platforms, on JS & WASM, only the function name is considered
 */
public data class PropRef(
    val property: KProperty1<*, *>,
) : PathSegment {
    override val pathString: String get() = ".${property.name}"

    override fun toString(): String = "PropRef(${property.name})"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as PropRef
        return callableEquals(property, other.property)
    }

    override fun hashCode(): Int = property.name.hashCode()
}

public fun KProperty1<*, *>.toPathSegment(): PropRef = PropRef(this)

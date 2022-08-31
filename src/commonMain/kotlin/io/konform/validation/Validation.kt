package io.konform.validation

import com.quickbirdstudios.nonEmptyCollection.list.NonEmptyList
import com.quickbirdstudios.nonEmptyCollection.unsafe.UnsafeNonEmptyCollectionApi
import com.quickbirdstudios.nonEmptyCollection.unsafe.toNonEmptyList

sealed class Properties {
    object Any : Properties()
    class Some(vararg properties: Path) : Properties() {
        val paths = properties.toList()
    }
}

infix fun Properties.contains(path: Path) = when (this) {
    Properties.Any -> true
    is Properties.Some -> this.paths.contains(path)
}

data class ErrorWithPath<E>(val path: Path, val error: E)

class Validation<T, E>(val block: Builder<T, E>.() -> Unit) {
    @OptIn(UnsafeNonEmptyCollectionApi::class)
    operator fun invoke(
        value: T,
        properties: Properties = Properties.Any
    ): Validated<T, NonEmptyList<ErrorWithPath<E>>> {
        val errors = mutableListOf<ErrorWithPath<E>>()

        try {
            ValidationBuilder(errors, Path(emptyList()), value, properties).block()
        } catch (_: StopValidation) {
        }

        return when (errors.isEmpty()) {
            true -> Valid(value)
            false -> Invalid(errors.toNonEmptyList())
        }
    }
}


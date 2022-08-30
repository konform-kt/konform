package io.konform.validation

import com.quickbirdstudios.nonEmptyCollection.list.NonEmptyList

typealias Valid<A> = Validated.Valid<A>

typealias Invalid<E> = Validated.Invalid<NonEmptyList<ErrorWithPath<E>>>

sealed class Validated<out A, out E> {
    data class Valid<out A>(val value: A) : Validated<A, Nothing>() {
        override fun toString(): String = "Validated.Valid($value)"
    }

    data class Invalid<out E>(val errors: E) : Validated<Nothing, E>() {
        override fun toString(): String = "Validated.Invalid($errors)"
    }
}

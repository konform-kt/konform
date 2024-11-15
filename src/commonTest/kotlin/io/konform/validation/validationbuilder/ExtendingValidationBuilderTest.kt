package io.konform.validation.validationbuilder

import io.konform.validation.Validation
import io.konform.validation.ValidationBuilder
import io.konform.validation.ValidationError
import io.konform.validation.constraints.minLength
import io.konform.validation.path.ValidationPath
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainExactlyErrors
import kotlin.reflect.KProperty1
import kotlin.test.Test

class ExtendingValidationBuilderTest {
    @Test
    fun allowsExtending() {
        val validation =
            MyValidationBuilder<User> {
                User::name trimmed {
                    minLength(1)
                }
            }

        validation shouldBeValid User("John")
        (validation shouldBeInvalid User("")).shouldContainExactlyErrors(
            ValidationError.of(User::name, "must have at least 1 characters"),
        )
        (validation shouldBeInvalid User("\t")).shouldContainExactlyErrors(
            ValidationError.of(User::name, "must have at least 1 characters"),
        )
    }
}

data class User(
    val name: String,
)

class MyValidationBuilder<T> : ValidationBuilder<T>() {
    infix fun KProperty1<T, String>.trimmed(init: ValidationBuilder<String>.() -> Unit): Unit =
        this {
            validate(ValidationPath.EMPTY, { it.trim() }) {
                run(buildWithNew(init))
            }
        }

    companion object {
        operator fun <T> invoke(init: MyValidationBuilder<T>.() -> Unit): Validation<T> {
            val builder = MyValidationBuilder<T>()
            init(builder)
            return builder.build()
        }
    }
}

package io.konform.validation.validationbuilder

import io.konform.validation.PropertyValidationError
import io.konform.validation.Validation
import io.konform.validation.string.notBlank
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainExactlyErrors
import kotlin.test.Test

class InstanceOfTest {
    private val catValidation =
        Validation<Cat> {
            Cat::favoritePrey {
                notBlank()
            }
        }

    private val ifCatValidation =
        Validation<Animal> {
            ifInstanceOf<Cat> {
                run(catValidation)
            }
        }

    private val requireCatValidation =
        Validation<Animal?> {
            requireInstanceOf<Cat> {
                run(catValidation)
            }
        }

    val validCat = Cat("cat", "mouse")
    val invalidCat = Cat("", "")
    val validDog = Dog("dog")
    val invalidDog = Dog("")

    @Test
    fun ifInstanceOfTest() {
        ifCatValidation shouldBeValid validCat
        ifCatValidation shouldBeValid validDog
        ifCatValidation shouldBeValid invalidDog
        ifCatValidation shouldBeValid null

        val invalid = ifCatValidation shouldBeInvalid invalidCat
        invalid shouldContainExactlyErrors
            listOf(
                PropertyValidationError(".favoritePrey", "must not be blank"),
            )
    }

    @Test
    fun requireInstanceOfTest() {
        requireCatValidation shouldBeValid validCat

        val invalidCatResult = requireCatValidation shouldBeInvalid invalidCat
        invalidCatResult shouldContainExactlyErrors
            listOf(
                PropertyValidationError(".favoritePrey", "must not be blank"),
            )

        val validDogResult = requireCatValidation shouldBeInvalid validDog
        val invalidDogResult = requireCatValidation shouldBeInvalid invalidDog
        val expectedError =
            listOf(
                PropertyValidationError("", "must be a 'Cat', was a 'Dog'"),
            )
        validDogResult shouldContainExactlyErrors expectedError
        invalidDogResult shouldContainExactlyErrors expectedError

        val nullResult = requireCatValidation shouldBeInvalid null
        nullResult shouldContainExactlyErrors
            listOf(
                PropertyValidationError("", "must be a 'Cat', was a 'null'"),
            )
    }
}

sealed interface Animal {
    val name: String
}

data class Cat(
    override val name: String,
    val favoritePrey: String,
) : Animal

data class Dog(
    override val name: String,
) : Animal

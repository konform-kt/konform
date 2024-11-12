package io.konform.validation.validationbuilder

import io.konform.validation.Validation
import io.konform.validation.ValidationError
import io.konform.validation.path.PathSegment
import io.konform.validation.path.ValidationPath
import io.konform.validation.string.notBlank
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainOnlyError
import kotlin.test.Test

class InstanceOfTest {
    private val catValidation =
        Validation<Cat> {
            Cat::favoritePrey {
                notBlank()
            }
        }

    private val ifCatValidation =
        Validation<Animal?> {
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
        invalid shouldContainOnlyError ValidationError.of(PathSegment.Prop(Cat::favoritePrey), "must not be blank")
    }

    @Test
    fun requireInstanceOfTest() {
        requireCatValidation shouldBeValid validCat

        val invalidCatResult = requireCatValidation shouldBeInvalid invalidCat
        invalidCatResult shouldContainOnlyError ValidationError.of(PathSegment.Prop(Cat::favoritePrey), "must not be blank")

        val validDogResult = requireCatValidation shouldBeInvalid validDog
        val invalidDogResult = requireCatValidation shouldBeInvalid invalidDog
        val expectedError = ValidationError(ValidationPath.EMPTY, "must be a 'Cat', was a 'Dog'")
        validDogResult shouldContainOnlyError expectedError
        invalidDogResult shouldContainOnlyError expectedError

        val nullResult = requireCatValidation shouldBeInvalid null
        nullResult shouldContainOnlyError ValidationError(ValidationPath.EMPTY, "must be a 'Cat', was a 'null'")
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

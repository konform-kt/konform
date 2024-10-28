package io.konform.validation

import io.konform.validation.jsonschema.minLength
import io.kotest.assertions.konform.shouldContainExactlyErrors
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.Test

class ValidationTest {
    interface Animal {
        val name: String
    }

    data class Cat(
        override val name: String,
        val favoritePrey: String,
    ) : Animal

    @Test
    fun validationsShouldBeUsableOnTypes() {
        val animalValidation: Validation<Animal> =
            Validation {
                Animal::name {
                    minLength(1)
                }
            }
        val catValidation: Validation<Cat> =
            Validation {
                run(animalValidation)
                Cat::favoritePrey {
                    minLength(1)
                }
            }

        val cat = Cat("Miss Kitty", "Mouse")
        val emptyCat = Cat("", "")

        animalValidation.validate(cat).isValid shouldBe true
        catValidation.validate(cat).isValid shouldBe true

        val invalidAnimal = animalValidation.validate(emptyCat)
        val invalidCat = catValidation.validate(emptyCat)
        invalidAnimal.shouldBeInstanceOf<Invalid>().shouldContainExactlyErrors(
            "name" to "min length 1",
        )
        invalidCat.shouldBeInstanceOf<Invalid>().shouldContainExactlyErrors(
            "name" to "min length 1",
            "favoritePrey" to "min length 1",
        )
    }
}

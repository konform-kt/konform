package io.konform.validation

import io.konform.validation.constraints.minLength
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainExactlyErrors
import io.kotest.assertions.konform.shouldContainOnlyError
import io.kotest.assertions.throwables.shouldThrow
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

    data class Dog(
        override val name: String,
    ) : Animal

    private val animalValidation: Validation<Animal> =
        Validation {
            Animal::name {
                minLength(1)
            }
        }
    private val catValidation: Validation<Cat> =
        Validation {
            run(animalValidation)
            Cat::favoritePrey {
                minLength(1)
            }
        }

    private val dogValidation: Validation<Dog> =
        Validation {
            run(animalValidation)
        }

    @Test
    fun validationsShouldBeUsableOnTypes() {
        // This is allowed and should compile, as every cat is an animal
        @Suppress("UNUSED_VARIABLE")
        val animalAsCatValidation: Validation<Cat> = animalValidation

        val cat = Cat("Miss Kitty", "Mouse")
        val emptyCat = Cat("", "")

        animalValidation.validate(cat).isValid shouldBe true
        catValidation.validate(cat).isValid shouldBe true

        val invalidAnimal = animalValidation.validate(emptyCat)
        val invalidCat = catValidation.validate(emptyCat)
        invalidAnimal.shouldBeInstanceOf<Invalid>().shouldContainOnlyError(
            ValidationError.of(Animal::name, "must have at least 1 characters"),
        )
        invalidCat.shouldBeInstanceOf<Invalid>().shouldContainExactlyErrors(
            ValidationError.of(Animal::name, "must have at least 1 characters"),
            ValidationError.of(Cat::favoritePrey, "must have at least 1 characters"),
        )
    }

    @Test
    fun andThen() {
        val catFirst: Validation<Cat> = catValidation andThen animalValidation
        val animalFirst: Validation<Cat> = animalValidation andThen catValidation

        val validCat = Cat("abc", "mouse")
        val invalidCat = Cat("abc", "")
        animalFirst shouldBeValid validCat
        catFirst shouldBeValid validCat
        (animalFirst shouldBeInvalid invalidCat) shouldContainOnlyError
            ValidationError.of(Cat::favoritePrey, "must have at least 1 characters")
        (catFirst shouldBeInvalid invalidCat) shouldContainOnlyError
            ValidationError.of(Cat::favoritePrey, "must have at least 1 characters")

        val chain =
            animalValidation andThen (
                object : Validation<Animal> {
                    override fun validate(value: Animal): ValidationResult<Animal> = throw IllegalStateException("should never be called")
                }
            )

        chain shouldBeInvalid Dog("")

        shouldThrow<IllegalStateException> {
            chain.validate(Dog("abc"))
        }
    }
}

package io.konform.validation.validationbuilder

import io.konform.validation.Validation
import io.konform.validation.string.notBlank
import kotlin.test.Test

class InstanceOfTest {
    private val alwaysFailValidation =
        Validation<Any?> {
            addConstraint("validation always fails") { false }
        }

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
    fun runsValidationIfCorrectType() {
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

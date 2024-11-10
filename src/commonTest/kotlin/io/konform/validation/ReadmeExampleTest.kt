package io.konform.validation

import io.konform.validation.jsonschema.maxItems
import io.konform.validation.jsonschema.maxLength
import io.konform.validation.jsonschema.maximum
import io.konform.validation.jsonschema.minItems
import io.konform.validation.jsonschema.minLength
import io.konform.validation.jsonschema.minimum
import io.konform.validation.jsonschema.pattern
import io.konform.validation.path.PathSegment.Property
import io.konform.validation.path.PathSegment.ProvidedString
import io.kotest.assertions.konform.shouldBeInvalid
import io.kotest.assertions.konform.shouldBeValid
import io.kotest.assertions.konform.shouldContainError
import kotlin.collections.Map.Entry
import kotlin.test.Test
import kotlin.test.assertEquals

class ReadmeExampleTest {
    data class UserProfile(
        val fullName: String,
        val age: Int?,
    )

    private val johnDoe = UserProfile("John Doe", 30)

    @Test
    fun simpleValidation() {
        val validateUser =
            Validation<UserProfile> {
                UserProfile::fullName {
                    minLength(2)
                    maxLength(100)
                }

                UserProfile::age ifPresent {
                    minimum(0)
                    maximum(150)
                }
            }

        val invalidUser = UserProfile("A", -1)
        val validationResult = validateUser(invalidUser)

        assertEquals(2, validationResult.errors.size)
        assertEquals("must have at least 2 characters", validationResult.errors.first().message)
        assertEquals("must be at least '0'", validationResult.errors.last().message)
    }

    @Test
    fun complexValidation() {
        data class Person(
            val name: String,
            val email: String?,
            val age: Int,
        )

        data class Event(
            val organizer: Person,
            val attendees: List<Person>,
            val ticketPrices: Map<String, Double?>,
        )

        val validateEvent =
            Validation<Event> {
                Event::organizer {
                    // even though the email is nullable you can force it to be set in the validation
                    Person::email required {
                        pattern("\\w+@bigcorp.com") hint "Organizers must have a BigCorp email address"
                    }
                }

                // validation on the attendees list
                Event::attendees {
                    maxItems(100)
                }

                // validation on individual attendees
                Event::attendees onEach {
                    Person::name {
                        minLength(2)
                    }
                    Person::age {
                        minimum(18) hint "Attendees must be 18 years or older"
                    }
                    // Email is optional but if it is set it must be valid
                    Person::email ifPresent {
                        pattern("\\w+@\\w+\\.\\w+") hint "Please provide a valid email address (optional)"
                    }
                }

                // validation on the ticketPrices Map as a whole
                Event::ticketPrices {
                    minItems(1) hint "Provide at least one ticket price"
                }

                // validations for the individual entries
                Event::ticketPrices onEach {
                    // Tickets may be free
                    Entry<String, Double?>::value ifPresent {
                        minimum(0.01)
                    }
                }
            }

        val validEvent =
            Event(
                organizer = Person("Organizer", "organizer@bigcorp.com", 30),
                attendees =
                    listOf(
                        Person("Visitor", null, 18),
                        Person("Journalist", "hello@world.com", 35),
                    ),
                ticketPrices =
                    mapOf(
                        "diversity-ticket" to null,
                        "early-bird" to 200.0,
                        "regular" to 400.0,
                    ),
            )

        assertEquals(Valid(validEvent), validateEvent(validEvent))

        val invalidEvent =
            Event(
                organizer = Person("Organizer", "organizer@smallcorp.com", 30),
                attendees =
                    listOf(
                        Person("Youngster", null, 17),
                    ),
                ticketPrices =
                    mapOf(
                        "we-pay-you" to -100.0,
                    ),
            )

        assertEquals(3, countFieldsWithErrors(validateEvent(invalidEvent)))
        assertEquals("Attendees must be 18 years or older", validateEvent(invalidEvent)[Event::attendees, 0, Person::age][0])
    }

    @Test
    fun customValidations() {
        val validateUser1 =
            Validation<UserProfile> {
                UserProfile::fullName {
                    addConstraint("Name cannot contain a tab") { !it.contains("\t") }
                }
            }

        validateUser1 shouldBeValid johnDoe
        validateUser1.shouldBeInvalid(UserProfile("John\tDoe", 30)) {
            it.shouldContainError(ValidationError.of(Property(UserProfile::fullName), "Name cannot contain a tab"))
        }

        val validateUser2 =
            Validation<UserProfile> {
                validate("trimmedName", { it.fullName.trim() }) {
                    minLength(5)
                }
            }

        validateUser2 shouldBeValid johnDoe
        validateUser2.shouldBeInvalid(UserProfile("J", 30)) {
            it.shouldContainError(ValidationError.of(ProvidedString("trimmedName"), "must have at least 5 characters"))
        }
    }

    @Test
    fun splitValidations() {
        val ageCheck =
            Validation<Int?> {
                required {
                    minimum(21)
                }
            }

        val validateUser =
            Validation<UserProfile> {
                UserProfile::age {
                    run(ageCheck)
                }
            }

        validateUser shouldBeValid johnDoe
        validateUser.shouldBeInvalid(UserProfile("John doe", 10)) {
            it.shouldContainError(ValidationError.of(Property(UserProfile::age), "must be at least '21'"))
        }

        val transform =
            Validation<UserProfile> {
                validate("ageMinus10", { it.age?.let { age -> age - 10 } }) {
                    run(ageCheck)
                }
            }

        transform shouldBeValid UserProfile("X", 31)
        transform.shouldBeInvalid(johnDoe) {
            it.shouldContainError(ValidationError.of(ProvidedString("ageMinus10"), "must be at least '21'"))
        }

        val required =
            Validation<UserProfile> {
                required("age", { it.age }) {
                    minimum(21)
                }
            }
        val optional =
            Validation<UserProfile> {
                ifPresent("age", { it.age }) {
                    minimum(21)
                }
            }
        val noAge = UserProfile("John Doe", null)
        required.shouldBeInvalid(noAge) {
            it.shouldContainError(ValidationError.of(ProvidedString("age"), "is required"))
        }
        optional.shouldBeValid(noAge)
        optional.shouldBeValid(johnDoe)
        optional.shouldBeInvalid(UserProfile("John Doe", 10)) {
            it.shouldContainError(ValidationError.of(ProvidedString("age"), "must be at least '21'"))
        }
    }
}

[![Test](https://github.com/konform-kt/konform/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/konform-kt/konform/actions/workflows/gradle.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.konform/konform/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.konform/konform)

# Portable validations for Kotlin

  - **✅ Type-safe DSL**
  - **🔗 Multi-platform support** (JVM, JS)
  - **🐥 Zero dependencies**

### Installation

For multiplatform projects:

```
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.konform:konform:0.4.0")
            }
        }
    }
}
```

For jvm-only projects add:

```
dependencies {
    implementation("io.konform:konform-jvm:0.4.0")
}
```

### Use

Suppose you have a data class like this:

```Kotlin
data class UserProfile(
    val fullName: String,
    val age: Int?
)
```

Using the Konform type-safe DSL you can quickly write up a validation

```Kotlin
val userValidation = Validation<UserProfile, ValidationError> {
    UserProfile::fullName {
        minLength(2)
        maxLength(100)
    }

    UserProfile::age ifPresent {
        minimum(0)
        maximum(150)
    }
}
```

and apply it to your data

```Kotlin
val invalidUser = UserProfile("A", -1)
val validationResult = userValidation.validate(invalidUser)
```

since the validation fails the `validationResult` will be of type `Invalid` and you can get a list of validation errors:

```Kotlin
validationResult.errors
// yields listOf(
//     ValidationError(dataPath=.fullName, message=must have at least 2 characters),
//     ValidationError(dataPath=.age, message=must be at least '0'
// )
```

In case the validation went through successfully you get a result of type `Valid` with the validated value in the `value` field.

```Kotlin
val validUser = UserProfile("Alice", 25)
val validationResult = userValidation.validate(validUser)
// yields Valid(UserProfile("Alice", 25))
```

### Advanced use

You can define validations for nested classes and use them for new validations

```Kotlin
val ageCheck = Validation<UserProfile, ValidationError> {
    require(UserProfile::age) {
        minimum(18)
    }
}

val validateUser = Validation<UserProfile, ValidationError> {
    UserProfile::fullName {
        minLength(2)
        maxLength(100)
    }
    
    run(ageCheck)
}
```

It is also possible to validate nested data classes and properties that are collections (List, Map, etc...)

```Kotlin
data class Error(override val message: String) : ValidationError

data class Person(val name: String, val email: String?, val age: Int)

data class Event(
    val organizer: Person,
    val attendees: List<Person>,
    val ticketPrices: Map<String, Double?>
)

val validateEvent = Validation<Event, ValidationError> {
    Event::organizer {
        // even though the email is nullable you can force it to be set in the validation
        require(Person::email) {
            pattern(".+@bigcorp.com") { ValidationError("Organizers must have a BigCorp email address") }
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
            minimum(18) { Error("Attendees must be 18 years or older") }
        }
        // Email is optional but if it is set it must be valid
        Person::email ifPresent {
            pattern(".+@.+\..+") { Error("Please provide a valid email address (optional)") }
        }
    }

    // validation on the ticketPrices Map as a whole
    Event::ticketPrices {
        minItems(1) { Error("Provide at least one ticket price") }
    }

    // validations for the individual entries
    Event::ticketPrices onEach {
        // Tickets may be free in which case they are null
        Entry<String, Double?>::value ifPresent {
            minimum(0.01)
        }
    }
}
```

### Other validation libraries written in Kotlin

  - Valikator: https://github.com/valiktor/valiktor
  - Kalidation: https://github.com/rcapraro/kalidation
  
### Integration with testing libraries

  - [Kotest](https://kotest.io) provides various matchers for use with Konform. They can be used in your tests to assert that a given object is validated successfully or fails validation with specific error messages. See [documentation](https://kotest.io/docs/assertions/konform-matchers.html).

##### Author

[Niklas Lochschmidt](https://twitter.com/niklas_l)

##### License

[MIT License](https://github.com/konform-kt/konform/blob/master/LICENSE)

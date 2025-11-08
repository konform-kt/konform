[![Test](https://github.com/konform-kt/konform/actions/workflows/release.yml/badge.svg?branch=main)](https://github.com/konform-kt/konform/actions/workflows/release.yml)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.konform/konform/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.konform/konform)

# Portable validations for Kotlin

- **✅ Type-safe DSL**
- **🔗 Multi-platform support** (JVM, JS, Native, Wasm)
- **🐥 Zero dependencies**

### Installation

For multiplatform projects:

```
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("io.konform:konform:0.11.0")
            }
        }
    }
}
```

For jvm-only projects add:

```
dependencies {
    implementation("io.konform:konform-jvm:0.11.0")
}
```

### Use

Suppose you have a data class like this:

```kotlin
data class UserProfile(
    val fullName: String,
    val age: Int?
)
```

Using the Konform type-safe DSL you can quickly write up a validation

```kotlin
val validateUser = Validation<UserProfile> {
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

```kotlin
val invalidUser = UserProfile("A", -1)
val validationResult = validateUser(invalidUser)
```

since the validation fails the `validationResult` will be of type `Invalid` and you can get a list of validation errors by indexed access:

```kotlin
validationResult.errors.messagesAtPath(UserProfile::fullName)
// yields listOf("must have at least 2 characters")

validationResult.errors.messagesAtPath(UserProfile::age)
// yields listOf("must be at least '0'")
```

or you can get all validation errors with details as a list:

```kotlin
validationResult.errors
// yields listOf(
//     ValidationError(path=ValidationPath(Prop(fullName)), message=must have at least 2 characters),
//     ValidationError(path=ValidationPath(Prop(age)), message=must be at least '0')
// )
```

In case the validation went through successfully you get a result of type `Valid` with the validated value in the `value` field.

```kotlin
val validUser = UserProfile("Alice", 25)
val validationResult = validateUser(validUser)
// yields Valid(UserProfile("Alice", 25))
```

### Detailed usage

#### Hints

You can add custom hints to validations

```kotlin
val validateUser = Validation<UserProfile> {
    UserProfile::age ifPresent {
        minimum(0) hint "Registering before birth is not supported"
    }
}
```

You can use `{value}` to include the `.toString()`-ed data in the hint

```kotlin
val validateUser = Validation<UserProfile> {
    UserProfile::fullName {
        minLength(2) hint "'{value}' is too short a name, must be at least 2 characters long."
    }
}
```

#### Custom context

You can add customs context to validation errors

```kotlin
val validateUser = Validation<UserProfile> {
    UserProfile::age {
        minimum(0) userContext Severity.ERROR
        // You can also set multiple things at once
        minimum(0).replace(
            hint = "Registering before birth is not supported",
            userContext = Severity.ERROR,
        )
    }
}
```

#### Custom validations

You can add custom validations on properties by using `constrain`

```kotlin
val validateUser = Validation<UserProfile> {
    UserProfile::fullName {
        constrain("Name cannot contain a tab") { !it.contains("\t") }
        // Set a custom path for the error
        constrain("Name must have a non-whitespace character", path = ValidationPath.of("trimmedName")) {
            it.trim().isNotEmpty()
        }
        // Set custom context
        constrain("Must have 5 characters", userContext = Severity.ERROR) {
            it.size >= 5
        }
    }
}
```

You can transform data and then add a validation on the result

```kotlin
val validateUser = Validation<UserProfile> {
    validate("trimmedName", { it.fullName.trim() }) {
        minLength(5)
    }
    // This also required and ifPresent for nullable values
    required("yourName", /* ...*/) {
        // your validations, giving an error out if the result is null
    }
    ifPresent("yourName", /* ... */) {
        // your validations, only running if the result is not null
    }
    // You can use a more extensive path, for example
    // the path will be ".fullName.trimmed" here:
    validate(ValidationPath.of(UserProfile::fullName, "trimmed"), { /* ... */ }) {
        /* ... */
    }
}
```

#### Configurable paths

You can override the path that appears in validation errors by setting the `path` property in a validation builder.
This is particularly useful for inline/value classes where you want the wrapper to be transparent in error paths,
or when the default path doesn't match your serialized structure (e.g., JSON):

```kotlin
@JvmInline
value class WrappedInt(val value: Int)

data class DataWithWrapper(val wrapped: WrappedInt)

val validation = Validation<DataWithWrapper> {
    DataWithWrapper::wrapped {
        // Remove the "wrapped" segment from the path
        path = ValidationPath.EMPTY
        validate("value", { it.value }) {
            minimum(1)
        }
    }
}

// Error path will be ".value" instead of ".wrapped.value"
validation(DataWithWrapper(WrappedInt(0)))
// yields Invalid with error at path "value"
```

You can also set a custom path:

```kotlin
val validation = Validation<DataWithWrapper> {
    DataWithWrapper::wrapped {
        path = ValidationPath.of("customPath")
        validate("value", { it.value }) {
            minimum(1)
        }
    }
}

// Error path will be ".customPath.value"
```

This works with all validation methods including `ifPresent`, `required`, and `onEach`.

#### Split validations

You can define validations separately and run them from other validations

```kotlin
val ageCheck = Validation<Int?> {
    required {
        minimum(21)
    }
}

val validateUser = Validation<UserProfile> {
    UserProfile::age {
        run(ageCheck)
    }

    // You can also transform the data and then run a validation against the result
    validate("ageMinus10", { it.age?.let { age -> age - 10 } }) {
        run(ageCheck)
    }
}
```

#### Collections

It is also possible to validate nested data classes and properties that are collections (List, Map, etc...)

```kotlin
data class Person(val name: String, val email: String?, val age: Int)

data class Event(
    val organizer: Person,
    val attendees: List<Person>,
    val ticketPrices: Map<String, Double?>
)

val validateEvent = Validation<Event> {
    Event::organizer {
        // even though the email is nullable you can force it to be set in the validation
        Person::email required {
            // Optionally set a hint, default hint is "is required"
            hint = "Email address must be given"
            // Optionally set a user context, default is `null`
            userContext = Severity.ERROR
            pattern(".+@bigcorp.com") hint "Organizers must have a BigCorp email address"
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
            pattern(".+@.+\..+") hint "Please provide a valid email address (optional)"
        }
    }

    // validation on the ticketPrices Map as a whole
    Event::ticketPrices {
        minItems(1) hint "Provide at least one ticket price"
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

Errors in the `ValidationResult` can also be accessed using the index access method. In case of `Iterables` and `Arrays` you use the
numerical index and in case of `Maps` you use the key as string.

```kotlin
// get the error messages for the first attendees age if any
result.errors.messagesAtPath(Event::attendees, 0, Person::age)

// get the error messages for the free ticket if any
result.errors.messagesAtPath(Event::ticketPrices, "free")
```

#### Dynamic Validations

Sometimes you want to create validations that depend on the context of the actual value being validated,
or define validations for fields that depend on other fields.
Note that this will generally have worse performance than using static validations.

```kotlin
Validation<Address> {
    Address::postalCode dynamic { address ->
        when (address.countryCode) {
            "US" -> pattern("[0-9]{5}")
            else -> pattern("[A-Z]+")
        }
    }
}
```

if you need to use a value further in, you can capture an earlier value with `dynamic`.

```kotlin
data class Numbers(val minimum: Int, val numbers: List<Int>)

Validation<Numbers> {
    dynamic { numbers ->
        Numbers::numbers onEach {
            minimum(numbers.minimum)
        }
    }
}
```

#### Subtypes

You can run validations only if the value is of a specific subtype, or require it to be specific subtype.

```kotlin
sealed interface Animal {
    val name: String
}
data class Cat(override val name: String, val favoritePrey: String) : Animal
data class Dog(override val name: String) : Animal

val validateAnimal = Validation<Animal> {
    Animal::name {
        notBlank()
    }
    // Only run this validation if the current Animal is a Cat and not null
    ifInstanceOf<Cat> {
        Cat::favoritePrey {
            notBlank()
        }
    }
}
val requireCat = Validation<Animal> {
    // This will return an invalid result is the current Animal is not a Cat or null
    requireInstanceOf<Cat> {
        Cat::favoritePrey {
            // ...
        }
    }
}
```

#### Recursive validation

If you have a recursive type that you can validate, this requires

1) an extra getter to get a self-reference to the validation, and
2) dynamic to create an extra instance of the validation as-needed to avoid an infinite loop

```kotlin
data class Node(val children: List<Node>)
val validation = Validation<Node> {
  // Use dynamic and a function to get the current validation again
  Node::children onEach {
    runDynamic { validationRef() }
  }
}
// Type must be explicitly specified on either this or the val
private val validationRef get(): Validation<Node> = validation
```

#### Fail-fast validations

Konform is primarily intended to validate the complete data and return all validation errors.
However, if you want to "fail fast" and not run later validations, you can do this with `andThen`
on `Validation` or `flatten` on a list of validations.

```kotlin
val fastValidation = Validation<String> { /* ... */ }
val slowValidation = Validation<String> { /* ... */ }

val runSlowOnlyIfFastValidationSucceeds = Validation<String> {
    run(fastValidation andThen slowValidation)
}
```

### Other validation libraries for Kotlin

- Akkurate: https://akkurate.dev/docs/overview.html
- Kalidation: https://github.com/rcapraro/kalidation
- Valikator: https://github.com/valiktor/valiktor

### Integration with testing libraries

- [Kotest](https://kotest.io) provides various matchers for use with Konform. They can be used in your tests to assert that a given object
  is validated successfully or fails validation with specific error messages.
  See [documentation](https://kotest.io/docs/assertions/konform-matchers.html).

##### Maintainer

[David Hoepelman](https://hoepelman.dev/) (Current maintainer)
[Niklas Lochschmidt](https://niklaslochschmidt.com) (Original author, co-maintainer)

##### License

[MIT License](https://github.com/konform-kt/konform/blob/main/LICENSE)

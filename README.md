[![Maven Central](https://maven-badges.herokuapp.com/maven-central/io.konform/konform/badge.svg)](https://maven-badges.herokuapp.com/maven-central/io.konform/konform) [![Build Status](https://travis-ci.org/konform-kt/konform.svg?branch=master)](https://travis-ci.org/konform-kt/konform)

# Portable validations for Kotlin

  - **✅ Type-safe DSL**
  - **🔗 Multi-platform support** (JVM, JS)
  - **🐥 Zero dependencies**
  - 🗣 i18n-support (*coming soon*)

### Installation

> TODO publish on Maven Central

### Quick Start

Suppose you have a data class like this:

```Kotlin
data class UserProfile(
    val fullName: String,
    val age: Int?
)
```

Using the Konform type-safe DSL you can quickly write up a validation

```Kotlin
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

```Kotlin
val validationResult = validateUser(UserProfile("A", -1))
```

since the validation fails the `validationResult` will be of type `Invalid` and you can get a list of validation errors by indexed access:

```Kotlin
validationResult[UserProfile::fullName]
// yields listOf("must be at least 2 characters")

validationResult[UserProfile::age]
// yields listOf("must be equal or greater than 0")
```


##### Author

 [Niklas Lochschmidt](https://twitter.com/niklas_l)

##### License

[MIT License](https://github.com/konform-kt/konform/blob/master/LICENSE)

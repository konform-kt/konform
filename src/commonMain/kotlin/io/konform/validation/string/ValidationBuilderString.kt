package io.konform.validation.string

import io.konform.validation.Constraint
import io.konform.validation.ValidationBuilder

public fun ValidationBuilder<String>.notBlank(): Constraint<String> = addConstraint("must not be blank") { it.isNotBlank() }

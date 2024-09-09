package io.konform.validation.kotlin

/**
 * Representation of parts of [the Kotlin grammar](https://kotlinlang.org/spec/syntax-and-grammar.html#lexical-grammar)
 */
internal object Grammar {
    private const val letter = "\\p{L}\\p{Nl}"  // Unicode letters (Lu, Ll, Lt, Lm, Lo)
    private const val unicodeDigit = "\\p{Nd}"  // Unicode digits (Nd)
    private const val quotedSymbol = "[^`\r\n]"  // Anything except backtick, CR, or LF inside backticks

    object Identifier {
        private const val STRING = "([${letter}_][${letter}_$unicodeDigit]*)|`$quotedSymbol+`"
        private val regex = "^$STRING$".toRegex()
        fun isValid(s: String) = s.matches(regex)
    }
}

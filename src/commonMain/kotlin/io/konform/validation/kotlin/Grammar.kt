package io.konform.validation.kotlin

/**
 * Representation of parts of [the Kotlin grammar](https://kotlinlang.org/spec/syntax-and-grammar.html#lexical-grammar)
 */
internal object Grammar {
    private const val LETTER = "\\p{L}\\p{Nl}" // Unicode letters (Lu, Ll, Lt, Lm, Lo)
    private const val UNICODE_DIGIT = "\\p{Nd}" // Unicode digits (Nd)
    private const val QUOTED_SYMBOL = "[^`\r\n]" // Anything except backtick, CR, or LF inside backticks

    object Identifier {
        internal const val STRING = "([${LETTER}_][${LETTER}_$UNICODE_DIGIT]*)|`$QUOTED_SYMBOL+`"
        private val regex = "^$STRING$".toRegex()

        fun isValid(s: String) = s.matches(regex)
    }

    object FunctionDeclaration {
        private const val UNARY_STRING = """(${Identifier.STRING})\(\)"""
        private val unaryRegex = "^$UNARY_STRING$".toRegex()

        fun isUnary(s: String) = s.matches(unaryRegex)
    }
}

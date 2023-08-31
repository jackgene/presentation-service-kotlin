package com.jackleow.presentation.flows.tokenizing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

class NormalizedWordsTokenizerSpec : WordSpec({
    val testAsciiText = "#hashtag hyphenated-word-  invalid_symbols?! YOLO Yo!fomo"
    val testUnicodeText = "Schrödinger's smol little 🐱 (小猫)!"
    val testWordLengthText = "i am not your large teapot"

    "A NormalizedWordsTokenizer" `when` {
        "configured with no stop words, minimum, or maximum word length" should {
            val instance = NormalizedWordsTokenizer()

            "correctly tokenize ASCII text" {
                // Test
                val actualTokens: List<String> = instance(testAsciiText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    "hashtag",          // # is not valid (considered whitespace)
                    "hyphenated-word",  // - is valid
                    "invalid",          // _ is not valid (considered whitespace)
                    "symbols",          // ? and ! are not valid (consider whitespace)
                    "yolo",             // lower-cased
                    "yo",               // ! is not valid (considered whitespace)
                    "fomo",             // after !
                )
                actualTokens shouldBe expectedTokens
            }

            "correctly tokenize Unicode text" {
                // Test
                val actualTokens: List<String> = instance(testUnicodeText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    "schrödinger",  // ' is not valid (considered whitespace)
                    "s",
                    "smol",
                    "little",       // 🐱 is not valid (considered whitespace)
                    "小猫",          // () are not valid (considered whitespace)
                )
                actualTokens shouldBe expectedTokens
            }

            "extract all words regardless of length tokenizing variable word length text" {
                // Test
                val actualTokens: List<String> = instance(testWordLengthText)

                // Verify
                val expectedTokens: List<String> = listOf("i", "am", "not", "your", "large", "teapot")
                actualTokens shouldBe expectedTokens
            }
        }

        "configured with no stop words, a minimum word length of 3, and no maximum word length" should {
            val instance = NormalizedWordsTokenizer(
                stopWords = setOf(), minWordLength = 3, maxWordLength = Int.MAX_VALUE
            )

            "omit short words tokenizing ASCII text" {
                // Test
                val actualTokens: List<String> = instance(testAsciiText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    "hashtag",
                    "hyphenated-word",
                    "invalid",
                    "symbols",
                    "yolo",
                    // "yo", too short
                    "fomo",
                )
                actualTokens shouldBe expectedTokens
            }

            "omit short words tokenizing Unicode text" {
                // Test
                val actualTokens: List<String> = instance(testUnicodeText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    "schrödinger",
                    // "s",   too short
                    "smol",
                    "little",
                    // "🐱"   not a letter
                    // "小猫"  too short
                )
                actualTokens shouldBe expectedTokens
            }

            "omit short words tokenizing variable word length text" {
                // Test
                val actualTokens: List<String> = instance(testWordLengthText)

                // Verify
                val expectedTokens: List<String> = listOf("not", "your", "large", "teapot")
                actualTokens shouldBe expectedTokens
            }
        }

        "configured with no stop words, no minimum word length, and a maximum word length of 4" should {
            val instance = NormalizedWordsTokenizer(
                stopWords = setOf(), minWordLength = 1, maxWordLength = 4
            )

            "omit long words tokenizing ASCII text" {
                // Test
                val actualTokens: List<String> = instance(testAsciiText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    // "hashtag",         too long
                    // "hyphenated-word", too long
                    // "invalid",         too long
                    // "symbols",         too long
                    "yolo",
                    "yo",
                    "fomo",
                )
                actualTokens shouldBe expectedTokens
            }

            "omit long words tokenizing Unicode text" {
                // Test
                val actualTokens: List<String> = instance(testUnicodeText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    // "schrödinger",  too long
                    "s",
                    "smol",
                    // "little",       too long
                    // "🐱"            not a letter
                    "小猫"
                )
                actualTokens shouldBe expectedTokens
            }

            "omit long words tokenizing variable word length text" {
                // Test
                val actualTokens: List<String> = instance(testWordLengthText)

                // Verify
                val expectedTokens: List<String> = listOf("i", "am", "not", "your")
                actualTokens shouldBe expectedTokens
            }
        }

        "configured with stop words and no minimum or maximum word length" should {
            val instance = NormalizedWordsTokenizer(stopWords = setOf("yolo", "large", "schrödinger"))

            "omit stop words tokenizing ASCII text" {
                // Test
                val actualTokens: List<String> = instance(testAsciiText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    "hashtag",
                    "hyphenated-word",
                    "invalid",
                    "symbols",
                    // "yolo", stop word
                    "yo",
                    "fomo",
                )
                actualTokens shouldBe expectedTokens
            }

            "omit stop words tokenizing Unicode text" {
                // Test
                val actualTokens: List<String> = instance(testUnicodeText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    //"schrödinger",  stop word
                    "s",
                    "smol",
                    "little",
                    // "🐱"           not a letter
                    "小猫",
                )
                actualTokens shouldBe expectedTokens
            }

            "omit stop words regardless of length when tokenizing variable word length text" {
                // Test
                val actualTokens: List<String> = instance(testWordLengthText)

                // Verify
                val expectedTokens: List<String> = listOf("i", "am", "not", "your", "teapot")
                actualTokens shouldBe expectedTokens
            }
        }

        "configured with stop words, a minimum word length of 3, and a maximum word length of 5" should {
            val instance = NormalizedWordsTokenizer(
                stopWords = setOf("yolo", "large", "schrödinger"),
                minWordLength = 3,
                maxWordLength = 5
            )

            "omit short, long, and stop words tokenizing ASCII text" {
                // Test
                val actualTokens: List<String> = instance(testAsciiText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    // "hashtag",          too long
                    // "hyphenated-word",  too long
                    // "invalid",          too long
                    // "symbols",          too long
                    // "yolo",             stop word
                    // "yo",               too short
                    "fomo",
                )
                actualTokens shouldBe expectedTokens
            }

            "omit short, long, and stop words tokenizing Unicode text" {
                // Test
                val actualTokens: List<String> = instance(testUnicodeText)

                // Verify
                val expectedTokens: List<String> = listOf(
                    // "schrödinger",  stop word
                    // "s",            too short
                    "smol",
                    // "little",       too long
                    // "🐱"            not a letter
                    // "小猫"           too short
                )
                actualTokens shouldBe expectedTokens
            }

            "omit short, long, and stop words tokenizing variable word length text" {
                // Test
                val actualTokens: List<String> = instance(testWordLengthText)

                // Verify
                val expectedTokens: List<String> = listOf("not", "your")
                actualTokens shouldBe expectedTokens
            }
        }

        "misconfigured" should {
            "fail on stop word of an empty string" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    NormalizedWordsTokenizer(setOf(""), 1, maxWordLength = Int.MAX_VALUE)
                }
            }

            "fail on stop word of a blank word" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    NormalizedWordsTokenizer(setOf(" "), 1, maxWordLength = Int.MAX_VALUE)
                }
            }

            "fail on stop word of a numeric string" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    NormalizedWordsTokenizer(setOf("1"), 1, maxWordLength = Int.MAX_VALUE)
                }
            }

            "fail on stop word containing non-letter symbols" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    NormalizedWordsTokenizer(setOf("\$_"), 1, maxWordLength = Int.MAX_VALUE)
                }
            }

            "fail on minimum word length less than 1" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    NormalizedWordsTokenizer(setOf(), 0, maxWordLength = Int.MAX_VALUE)
                }
            }

            "fail on maximum word length less than minimum word length" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    NormalizedWordsTokenizer(setOf(), 5, 4)
                }
            }
        }
    }
})

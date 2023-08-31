package com.jackleow.presentation.flows.tokenizing

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.shouldBe

class MappedKeywordsTokenizerSpec : WordSpec({
    val testText = "Lorem ipsum dolor sit amet!"

    "A MappedKeywordsTokenizer" `when` {
        "configured with lower-case keyed mapping" should {
            val instance = MappedKeywordsTokenizer(
                mapOf(
                    "lorem" to "Mock-1",
                    "ipsum" to "Mock-1",
                    "amet" to "Mock-2",
                    "other" to "Mock-2",
                )
            )

            "extract matching keywords" {
                // Test
                val actualTokens = instance(testText)

                // Verify
                val expectedTokens = listOf("Mock-1", "Mock-1", "Mock-2")
                actualTokens shouldBe expectedTokens
            }
        }

        "configured with upper-case keyed mapping" should {
            val instance = MappedKeywordsTokenizer(
                mapOf(
                    "LOREM" to "Mock-1",
                    "IPSUM" to "Mock-1",
                    "AMET" to "Mock-2",
                    "OTHER" to "Mock-2",
                )
            )

            "extract matching keywords" {
                // Test
                val actualTokens = instance(testText)

                // Verify
                val expectedTokens = listOf("Mock-1", "Mock-1", "Mock-2")
                actualTokens shouldBe expectedTokens
            }
        }

        "misconfigured" should {
            "fail on an empty mapping" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf())
                }
            }

            "fail on mapping with space in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock token" to "whatever"))
                }
            }

            "fail on mapping with tab in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock\ttoken" to "whatever"))
                }
            }

            "fail on mapping with exclamation mark in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock!token" to "whatever"))
                }
            }

            "fail on mapping with quote in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock\"token" to "whatever"))
                }
            }

            "fail on mapping with ampersand in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock&token" to "whatever"))
                }
            }

            "fail on mapping with comma in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock,token" to "whatever"))
                }
            }

            "fail on mapping with period in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock.token" to "whatever"))
                }
            }

            "fail on mapping with slash in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock/token" to "whatever"))
                }
            }

            "fail on mapping with question mark in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock?token" to "whatever"))
                }
            }

            "fail on mapping with pipe in raw token" {
                // Test
                shouldThrow<IllegalArgumentException> {
                    MappedKeywordsTokenizer(mapOf("mock|token" to "whatever"))
                }
            }
        }
    }
})

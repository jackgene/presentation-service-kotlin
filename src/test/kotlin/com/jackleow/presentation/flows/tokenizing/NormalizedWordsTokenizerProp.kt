package com.jackleow.presentation.flows.tokenizing

import com.jackleow.presentation.KotestProjectConfig
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldNotContain
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThanOrEqual
import io.kotest.matchers.should
import io.kotest.property.Arb
import io.kotest.property.arbitrary.positiveInt
import io.kotest.property.arbitrary.set
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll

@OptIn(ExperimentalKotest::class)
class NormalizedWordsTokenizerProp : WordSpec({
    concurrency = KotestProjectConfig.parallelism

    "A NormalizedWordsTokenizer" should {
        "only extract hyphenated lower-case tokens" {
            checkAll(Arb.string()) { text: String ->
                // Set up
                val instance = NormalizedWordsTokenizer()

                // Test
                val actualTokens: List<String> = instance(text)

                // Verify
                for (actualToken: String in actualTokens) {
                    for (c: Char in actualToken) {
                        c should { it == '-' || it.isLowerCase() }
                    }
                }
            }
        }

        "never extract stop words" {
            checkAll(
                Arb.set(Arb.stringPattern("[a-z]+"), 1..100), Arb.string()
            ) { stopWords: Set<String>, text: String ->
                // Set up
                val instance = NormalizedWordsTokenizer(stopWords)

                // Test
                val actualTokens: List<String> = instance(text)

                // Verify
                for (actualToken: String in actualTokens) {
                    stopWords shouldNotContain actualToken
                }
            }
        }

        "only extract words longer than minWordLength" {
            checkAll(
                Arb.positiveInt(), Arb.string()
            ) { minWordLength: Int, text: String ->
                // Set up
                val instance = NormalizedWordsTokenizer(minWordLength = minWordLength)

                // Test
                val actualTokens: List<String> = instance(text)

                // Verify
                for (actualToken: String in actualTokens) {
                    actualToken.length shouldBeGreaterThanOrEqual minWordLength
                }
            }
        }

        "only extract words shorter than maxWordLength" {
            checkAll(
                Arb.positiveInt(), Arb.string()
            ) { maxWordLength: Int, text: String ->
                // Set up
                val instance = NormalizedWordsTokenizer(maxWordLength = maxWordLength)

                // Test
                val actualTokens: List<String> = instance(text)

                // Verify
                for (actualToken: String in actualTokens) {
                    actualToken.length shouldBeLessThanOrEqual maxWordLength
                }
            }
        }
    }
})

package com.jackleow.presentation.flows.tokenizing

import com.jackleow.presentation.KotestProjectConfig
import io.kotest.common.ExperimentalKotest
import io.kotest.core.spec.style.WordSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.property.Arb
import io.kotest.property.Gen
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.arbitrary.stringPattern
import io.kotest.property.checkAll

@OptIn(ExperimentalKotest::class)
class MappedKeywordsTokenizerProp : WordSpec({
    concurrency = KotestProjectConfig.parallelism

    "A MappedKeywordsTokenizer" should {
        val keywordsByRawToken: Gen<Map<String, String>> =
            Arb.map(Arb.stringPattern("[a-z]+"), Arb.string(), 1)

        "extract all mapped tokens" {
            checkAll(keywordsByRawToken) { keywordsByRawToken: Map<String, String> ->
                // Set up
                val instance = MappedKeywordsTokenizer(keywordsByRawToken)

                // Test
                val actualTokens: List<String> = instance(keywordsByRawToken.keys.joinToString(" "))

                // Verify
                assert(actualTokens.toSet() == keywordsByRawToken.values.toSet())
            }
        }

        "only extract mapped tokens" {
            checkAll(
                keywordsByRawToken, Arb.string()
            ) { keywordsByRawToken: Map<String, String>, text: String ->
                // Set up
                val instance = MappedKeywordsTokenizer(keywordsByRawToken)

                // Test
                val actualTokens: List<String> = instance("$text ${keywordsByRawToken.keys.first()}")

                // Verify
                keywordsByRawToken.values.toSet() shouldContainAll actualTokens.toSet()
            }
        }
    }
})

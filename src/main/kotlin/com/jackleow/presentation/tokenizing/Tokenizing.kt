package com.jackleow.presentation.tokenizing

typealias Tokenizer = (String) -> List<String>

object Tokenizing {
    fun mappedKeywordsTokenizer(keywordsByRawToken: Map<String, String>): Tokenizer {
        require(keywordsByRawToken.isNotEmpty()) { "keywordsByRawToken must not be empty" }
        val wordSeparatorPattern = Regex("""[\s!"&,./?|]""")
        val invalidRawTokens: List<String> = keywordsByRawToken.keys
            .filter { wordSeparatorPattern.find(it) != null }
        require(invalidRawTokens.isEmpty()) {
            "some keyword mappings have invalid raw tokens: ${invalidRawTokens.joinToString(",", "{", "}")}"
        }

        val keywordsByLowerCasedToken: Map<String, String> = keywordsByRawToken.mapKeys { it.key.lowercase() }

        return { text ->
            wordSeparatorPattern.split(text.trim())
                .mapNotNull { keywordsByLowerCasedToken[it.lowercase()] }
        }
    }

    fun normalizedWordsTokenizer(stopWords: Set<String> = setOf(), minWordLength: Int = 1): Tokenizer {
        require(minWordLength >= 1) { "minWordLength must be at least 1" }
        val validWordPattern = Regex("""(\p{L}+(?:-\p{L}+)*)""")
        val invalidStopWords: List<String> = stopWords.filterNot { (validWordPattern.matches(it)) }
        require(invalidStopWords.isEmpty()) {
            "some stop words are invalid: ${invalidStopWords.joinToString(",", "{", "}")}"
        }
        val wordSeparatorPattern = Regex("""[^\p{L}\-]+""")

        val lowerCasedStopWords: Set<String> = stopWords.map { it.lowercase() }.toSet()

        return { text ->
            wordSeparatorPattern.split(text.trim())
                .map { it.lowercase() }
                .filter {
                    validWordPattern.matches(it) && it.length >= minWordLength && !lowerCasedStopWords.contains(it)
                }
        }
    }
}
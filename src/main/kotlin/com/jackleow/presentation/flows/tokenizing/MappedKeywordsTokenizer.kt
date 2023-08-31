package com.jackleow.presentation.flows.tokenizing

class MappedKeywordsTokenizer(keywordsByRawToken: Map<String, String>) : Tokenizer {
    companion object {
        private val wordSeparatorPattern: Regex = Regex("""[\s!"&,./?|]""")
    }

    init {
        require(keywordsByRawToken.isNotEmpty()) { "keywordsByRawToken must not be empty" }
        val invalidRawTokens: List<String> = keywordsByRawToken.keys
            .filter { wordSeparatorPattern.find(it) != null }
        require(invalidRawTokens.isEmpty()) {
            "some keyword mappings have invalid raw tokens: ${
                invalidRawTokens.joinToString(",", "{", "}")
            }"
        }
    }

    private val keywordsByLowerCasedToken: Map<String, String> =
        keywordsByRawToken.mapKeys { it.key.lowercase() }

    override fun invoke(text: String): List<String> =
        wordSeparatorPattern.split(text.trim())
            .mapNotNull { keywordsByLowerCasedToken[it.lowercase()] }
}

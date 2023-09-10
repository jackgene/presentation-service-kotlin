package com.jackleow.presentation.flows.tokenizing

class NormalizedWordsTokenizer(
    stopWords: Set<String> = setOf(),
    private val minWordLength: Int = 1,
    private val maxWordLength: Int = Int.MAX_VALUE
) : Tokenizer {
    companion object {
        private val validWordPattern = Regex("""(\p{L}+(?:-\p{L}+)*)""")
        private val wordSeparatorPattern = Regex("""[^\p{L}\-]+""")
    }

    init {
        require(minWordLength >= 1) { "minWordLength ($minWordLength) must be at least 1" }
        require(maxWordLength >= minWordLength) {
            "maxWordLength ($maxWordLength) must be no less than minWordLength ($minWordLength)"
        }
        val invalidStopWords: List<String> = stopWords.filterNot(validWordPattern::matches)
        require(invalidStopWords.isEmpty()) {
            "some stop words are invalid: ${invalidStopWords.joinToString(",", "{", "}")}"
        }
    }

    private val lowerCasedStopWords: Set<String> = stopWords.map { it.lowercase() }.toSet()

    override fun invoke(text: String): List<String> =
        wordSeparatorPattern.split(text.trim())
            .map { it.lowercase().trim('-') }
            .filter {
                validWordPattern.matches(it)
                        && it.length in minWordLength..maxWordLength
                        && !lowerCasedStopWords.contains(it)
            }
}

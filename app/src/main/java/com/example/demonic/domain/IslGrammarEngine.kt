package com.example.demonic.domain

enum class PosTag {
    SUBJECT, OBJECT, VERB, NEGATION, QUESTION, AUXILIARY, UNKNOWN
}

data class Token(
    val text: String,
    val pos: PosTag = PosTag.UNKNOWN
)

interface GrammarRule {
    fun apply(tokens: List<Token>): List<Token>
}

class IslGrammarEngine {

    private val lexicon = mutableMapOf<String, PosTag>().apply {
        // Subjects
        put("i", PosTag.SUBJECT)
        put("you", PosTag.SUBJECT)
        put("he", PosTag.SUBJECT)
        put("she", PosTag.SUBJECT)
        put("we", PosTag.SUBJECT)
        put("they", PosTag.SUBJECT)
        put("my", PosTag.SUBJECT)
        put("your", PosTag.SUBJECT)
        put("his", PosTag.SUBJECT)
        put("her", PosTag.SUBJECT)
        put("our", PosTag.SUBJECT)
        put("their", PosTag.SUBJECT)

        // Objects
        put("banana", PosTag.OBJECT)
        put("friend", PosTag.OBJECT)
        put("brother", PosTag.OBJECT)
        put("sister", PosTag.OBJECT)
        put("wife", PosTag.OBJECT)
        put("husband", PosTag.OBJECT)
        put("coffee", PosTag.OBJECT)
        put("job", PosTag.OBJECT)
        put("library", PosTag.OBJECT)
        put("son", PosTag.OBJECT)
        put("daughter", PosTag.OBJECT)

        // Verbs
        put("eat", PosTag.VERB)
        put("eats", PosTag.VERB)
        put("ate", PosTag.VERB)
        put("eating", PosTag.VERB)
        put("go", PosTag.VERB)
        put("went", PosTag.VERB)
        put("teach", PosTag.VERB)
        put("closed", PosTag.VERB)

        // Auxiliaries / Copulas (omitted in sign language glosses)
        put("is", PosTag.AUXILIARY)
        put("am", PosTag.AUXILIARY)
        put("are", PosTag.AUXILIARY)
        put("was", PosTag.AUXILIARY)
        put("were", PosTag.AUXILIARY)
        put("do", PosTag.AUXILIARY)
        put("does", PosTag.AUXILIARY)
        put("did", PosTag.AUXILIARY)

        // Negations
        put("not", PosTag.NEGATION)
        put("no", PosTag.NEGATION)
        put("never", PosTag.NEGATION)

        // Questions
        put("who", PosTag.QUESTION)
        put("where", PosTag.QUESTION)
        put("when", PosTag.QUESTION)
        put("why", PosTag.QUESTION)
        put("what", PosTag.QUESTION)
        put("how", PosTag.QUESTION)
    }

    private val rules = mutableListOf<GrammarRule>().apply {
        add(QuestionRule())
        add(NegationRule())
        add(SovRule())
        add(FilterAuxiliariesRule())
    }

    fun addRule(rule: GrammarRule) {
        rules.add(rule)
    }

    fun registerLexiconWord(word: String, tag: PosTag) {
        lexicon[word.lowercase()] = tag
    }

    fun transform(englishText: String, knownPhrases: List<String> = emptyList()): List<String> {
        val cleaned = englishText.lowercase()
            .replace(Regex("[.,?!;:()\\-\"]"), "")
            .trim()

        if (cleaned.isEmpty()) return emptyList()

        // 1. Tokenize using phrase-aware tokenizer on original clean text
        val initialTokens = tokenize(cleaned, knownPhrases)

        // 2. Expand contractions on single-word tokens
        val tokens = mutableListOf<Token>()
        for (token in initialTokens) {
            if (token.text.contains(" ") || token.text.contains("-")) {
                tokens.add(token)
            } else {
                val expanded = when (token.text) {
                    "don't", "dont" -> listOf("do", "not")
                    "doesn't", "doesnt" -> listOf("does", "not")
                    "didn't", "didnt" -> listOf("did", "not")
                    "i'm", "im" -> listOf("i", "am")
                    else -> listOf(token.text)
                }
                for (word in expanded) {
                    tokens.add(Token(word, lexicon[word] ?: PosTag.UNKNOWN))
                }
            }
        }

        var processedTokens: List<Token> = tokens
        for (rule in rules) {
            processedTokens = rule.apply(processedTokens)
        }

        return processedTokens.map { token ->
            if (token.pos == PosTag.NEGATION) "no" else token.text
        }
    }

    private fun tokenize(text: String, knownPhrases: List<String>): List<Token> {
        val result = mutableListOf<Token>()
        var remaining = text.trim()

        val cleanPhrases = knownPhrases.map { 
            it.lowercase().replace("_", " ").replace("-", " ").trim()
        }.filter { it.isNotEmpty() }.distinct().sortedByDescending { it.length }

        while (remaining.isNotEmpty()) {
            var matchedPhrase: String? = null
            for (phrase in cleanPhrases) {
                if (remaining.startsWith(phrase)) {
                    if (remaining.length == phrase.length || remaining[phrase.length] == ' ') {
                        matchedPhrase = phrase
                        break
                    }
                }
            }

            if (matchedPhrase != null) {
                result.add(Token(matchedPhrase, lexicon[matchedPhrase] ?: PosTag.UNKNOWN))
                remaining = remaining.substring(matchedPhrase.length).trim()
            } else {
                val spaceIndex = remaining.indexOf(' ')
                val word = if (spaceIndex != -1) remaining.substring(0, spaceIndex) else remaining
                result.add(Token(word, lexicon[word] ?: PosTag.UNKNOWN))
                remaining = if (spaceIndex != -1) remaining.substring(spaceIndex).trim() else ""
            }
        }
        return result
    }

    private class QuestionRule : GrammarRule {
        override fun apply(tokens: List<Token>): List<Token> {
            val hasQuestion = tokens.any { it.pos == PosTag.QUESTION }
            if (!hasQuestion) return tokens

            val result = mutableListOf<Token>()
            val questionTokens = mutableListOf<Token>()

            for (token in tokens) {
                if (token.pos == PosTag.QUESTION) {
                    questionTokens.add(token)
                } else {
                    result.add(token)
                }
            }
            result.addAll(questionTokens)
            return result
        }
    }

    private class NegationRule : GrammarRule {
        override fun apply(tokens: List<Token>): List<Token> {
            val hasNegation = tokens.any { it.pos == PosTag.NEGATION }
            if (!hasNegation) return tokens

            val result = mutableListOf<Token>()
            val negationTokens = mutableListOf<Token>()

            for (token in tokens) {
                if (token.pos == PosTag.NEGATION) {
                    negationTokens.add(token)
                } else {
                    result.add(token)
                }
            }

            val questionIndex = result.indexOfFirst { it.pos == PosTag.QUESTION }
            if (questionIndex != -1) {
                result.addAll(questionIndex, negationTokens)
            } else {
                result.addAll(negationTokens)
            }
            return result
        }
    }

    private class SovRule : GrammarRule {
        override fun apply(tokens: List<Token>): List<Token> {
            val result = tokens.toMutableList()

            val sIdx = result.indexOfFirst { it.pos == PosTag.SUBJECT }
            val vIdx = result.indexOfFirst { it.pos == PosTag.VERB }
            val oIdx = result.indexOfFirst { it.pos == PosTag.OBJECT }

            if (sIdx != -1 && vIdx != -1 && oIdx != -1 && sIdx < vIdx && vIdx < oIdx) {
                val verbToken = result.removeAt(vIdx)
                val newOIdx = oIdx - 1
                result.add(newOIdx + 1, verbToken)
            }
            return result
        }
    }

    private class FilterAuxiliariesRule : GrammarRule {
        override fun apply(tokens: List<Token>): List<Token> {
            return tokens.filter { it.pos != PosTag.AUXILIARY }
        }
    }
}

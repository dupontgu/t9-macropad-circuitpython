package com.dupontgu.t9

enum class Languages(val languageCode: String, val librarySourceUrl: String) {
    ENGLISH(
        "en",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/english.txt"
    ),
    SPANISH(
        "es",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/spanish.txt"
    ),
    ITALIAN(
        "it",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/italian.txt"
    ),
    SWEDISH(
        "sw",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/swedish.txt"
    ),
    GERMAN(
        "de",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/german.txt"
    ),
    FRENCH(
        "fr",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/french.txt"
    ),
    DUTCH(
        "nl",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/dutch.txt"
    ),
    DANISH(
        "da",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/danish.txt"
    ),
    PORTUGUESE(
        "pt",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/portuguese.txt"
    ),
    NORWEGIAN(
        "nb",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/norwegian.txt"
    ),
    ALBANIAN(
        "sq",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/albanian.txt"
    ),
    CATALAN(
        "ca",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/catalan.txt"
    ),
    ESTONIAN(
        "et",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/estonian.txt"
    ),
    FINNISH(
        "fi",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/finnish.txt"
    ),
    INDONESIAN(
        "id",
        "https://raw.githubusercontent.com/oprogramador/most-common-words-by-language/master/src/resources/indonesian.txt"
    ),
}

/**
 * Map of physical T9 buttons to set of type-able characters
 * Note - each language should only keep the characters it needs.
 * Keeping extra characters around will slow down search time.
 */
val globalKeyMap = mapOf(
    '1' to listOf('1'),
    '2' to listOf('a', 'b', 'c', 'à', 'á', 'â', 'ä', 'æ', 'ã', 'å', 'ā', 'ç', 'ć', 'č'),
    '3' to listOf('d', 'e', 'f', 'ð', 'è', 'é', 'ê', 'ë', 'ē', 'ė', 'ę'),
    '4' to listOf('g', 'h', 'i', 'î', 'ï', 'í', 'ī', 'į', 'ì'),
    '5' to listOf('j', 'k', 'l', '\'', '-'),
    '6' to listOf('m', 'n', 'o', 'ñ', 'ń', 'ô', 'ö', 'ò', 'ó', 'œ', 'ø', 'ō', 'õ'),
    '7' to listOf('p', 'q', 'r', 's', 'ß', 'ś', 'š'),
    '8' to listOf('t', 'u', 'v', 'þ', 'û', 'ü', 'ù', 'ú', 'ū'),
    '9' to listOf('w', 'x', 'y', 'z'),
    '0' to listOf(' ', '0', '\n'),
    '#' to listOf('.', ',', '?', '!', '¿', '¡')
)

// These characters should be available in every language
val universalCharacters = listOf(globalKeyMap['1']!!, globalKeyMap['0']!!, globalKeyMap['#']!!).flatten()

// IMPORTANT - do not change the order of this between releases
// any new characters should be appended to the end
val charMap = (
        ('a'..'z') + listOf(
            'ü',
            'é',
            'ç',
            'á',
            'í',
            'ó',
            'ú',
            'ñ',
            'à',
            'è',
            'ì',
            'ù',
            'ò',
            'õ',
            'ð',
            'š',
            'þ',
            'ê',
            'î',
            'ï',
            'ã',
            'ä',
            'ö',
            'ô',
            'å',
            'â',
            'ß',
            'ë',
            'ø',
            'æ',
            '\'',
            '-'
        ).sorted())
    .mapIndexed { i, c -> c to i }
    .toMap()
    .onEach { (key, _) ->
        if(globalKeyMap.none { it.value.contains(key)}) {
            throw RuntimeException("Missing key mapping for character: $key!")
        }
    }




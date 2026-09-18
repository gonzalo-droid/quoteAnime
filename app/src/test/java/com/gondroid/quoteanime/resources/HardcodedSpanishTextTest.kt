package com.gondroid.quoteanime.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * No text the user can see is written in Spanish inside the Kotlin code: it goes to
 * `values/strings.xml` (English) and `values-es/strings.xml` (Spanish). A literal in the code is
 * shown in Spanish to everyone, whatever the device language — the bug this pins.
 *
 * **Why a source scan.** The alternatives were weighed:
 *  - Android Lint's `HardcodedText` only looks at XML layouts, never at Compose.
 *  - A custom Lint detector needs its own module pinned to the AGP's lint-api, and runs under
 *    `lint`, not under `testDebugUnitTest`, which is the check every merge goes through.
 *  - A Compose UI test in English only sees the screens and states it visits, and needs a device.
 *  - "Every `Text` takes a `stringResource`" can't tell UI text from data (RTDB ids, routes).
 *
 * Reading the sources from a plain JVM test sees every state of every screen, costs milliseconds
 * and needs no allowlist for English identifiers, log tags or routes, because it looks for
 * *Spanish*, not for literals. A literal is reported when it
 *  1. has a character English never uses (á é í ó ú ñ ü ¿ ¡), or
 *  2. has two or more words and a Spanish function word ("Paso 1 de 3"), or
 *  3. is exactly one of the Spanish translations in `values-es` that differs from the English
 *     one — this catches single words without accents ("Amor", "Favoritos") that 1 and 2 miss.
 *
 * Not user-visible, so skipped: comments, annotation arguments (`@Preview(name = "…")`),
 * top-level declarations annotated `@Preview` or named `sample…`/`preview…` (preview fixtures),
 * and any line marked `i18n-ignore` — for data that happens to be Spanish, like the RTDB
 * category ids (`"motivación"`), with the reason next to the marker.
 *
 * Scope: `presentation/`, `widget/` and `notification/` — everything that draws text.
 *
 * Scenarios covered:
 *  - The app's sources have no Spanish literal outside the exclusions
 *  - Positive control: each of the three rules catches its case in a sample source
 *  - Negative control: comments, previews, fixtures, annotation arguments, `i18n-ignore`,
 *    English text and identifiers are not reported
 */
class HardcodedSpanishTextTest {

    @Test
    fun `given the UI sources, when scanned, then no Spanish text is hardcoded`() {
        val scanner = SpanishLiteralScanner(spanishOnlyTranslations())
        val offenders = SCANNED_DIRS
            .flatMap { dir -> sourceRoot().resolve(dir).walkTopDown().filter { it.extension == "kt" }.toList() }
            .sortedBy { it.path }
            .flatMap { file ->
                scanner.scan(file.readText()).map { "${file.relativeTo(sourceRoot())}:${it.line + 1}: \"${it.text}\"" }
            }

        assertEquals(
            "Spanish text hardcoded in the code — move it to values/ and values-es/ strings.xml " +
                "(or mark real data with `// i18n-ignore: <reason>`):\n" + offenders.joinToString("\n"),
            emptyList<String>(),
            offenders
        )
    }

    @Test
    fun `given Spanish UI text in a sample source, when scanned, then each rule reports it`() {
        val source = """
            @Composable
            fun Screen(page: Int, total: Int) {
                Text("Cómo agregar el widget")
                Text(text = "Paso ${'$'}{page + 1} de ${'$'}total")
                Icon(contentDescription = "Favoritos")
                val steps = listOf(Step(title = "¡Listo!"))
            }
        """.trimIndent()

        val found = SpanishLiteralScanner(setOf("Favoritos")).scan(source).map { it.text }

        assertEquals(
            listOf("Cómo agregar el widget", "Paso de", "Favoritos", "¡Listo!"),
            found
        )
    }

    @Test
    fun `given non-UI or English literals, when scanned, then nothing is reported`() {
        val source = """
            // Texto de ayuda: "Mantén presionado el widget"
            /* "Toca para reintentar" */
            @Preview(name = "Home — cargando", showBackground = true)
            @Composable
            private fun PreviewHome() {
                Text("Sin frases disponibles.")
            }

            private val sampleQuote = Quote(
                quote = "No mires atrás. Si lo haces ya has perdido."
            )

            private fun previewState() = State(title = "Entrenar como Deku")

            @Composable
            fun Screen() {
                Log.d("SettingsViewModel", "load failed")
                Text(stringResource(R.string.home_empty))
                navigate("home?quoteId=${'$'}id")
                val id = "motivación" // i18n-ignore: RTDB category id
                Text("Quote Anime")
                @Suppress("Súper") val x = 1
            }
        """.trimIndent()

        val found = SpanishLiteralScanner(setOf("Favoritos")).scan(source)

        assertTrue("Unexpected: $found", found.isEmpty())
    }

    /** Spanish translations that are not also the English text ("Widget", "Premium"). */
    private fun spanishOnlyTranslations(): Set<String> {
        val english = StringResourceFile.load("values").groupBy({ it.name }, { it.text })
        return StringResourceFile.load("values-es")
            .filter { entry -> english[entry.name]?.contains(entry.text) != true }
            .map { it.text.trim() }
            .filter { it.isNotEmpty() && '%' !in it }
            .toSet()
    }

    private fun sourceRoot(): File =
        listOf(File(SOURCE_ROOT), File("app/$SOURCE_ROOT")).firstOrNull { it.isDirectory }
            ?: error("Could not find $SOURCE_ROOT from ${File(".").absolutePath}")

    private companion object {
        const val SOURCE_ROOT = "src/main/java/com/gondroid/quoteanime"
        val SCANNED_DIRS = listOf("presentation", "widget", "notification")
    }
}

/**
 * Finds string literals that read as Spanish in a Kotlin source. See [HardcodedSpanishTextTest]
 * for the rules and the exclusions. Template expressions (`${…}`, `$name`) count as a blank.
 */
internal class SpanishLiteralScanner(private val spanishTranslations: Set<String>) {

    /** [line] is 0-based; [text] has templates blanked out and whitespace collapsed. */
    data class Literal(val line: Int, val text: String)

    fun scan(source: String): List<Literal> =
        topLevelChunks(source)
            .filterNot { it.isPreviewOnly() }
            .flatMap { chunk -> literals(chunk.text).map { it.copy(line = it.line + chunk.firstLine) } }
            .filter { literal -> !isIgnoredLine(source, literal.line) && looksSpanish(literal.text) }
            .map { it.copy(text = it.text.replace(WHITESPACE, " ").trim()) }

    fun looksSpanish(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return false
        if (SPANISH_ONLY_CHARS.containsMatchIn(trimmed)) return true
        val words = WORD.findAll(trimmed).map { it.value.lowercase() }.toList()
        if (words.size >= 2 && words.any { it in FUNCTION_WORDS }) return true
        return trimmed in spanishTranslations
    }

    // ── Top-level chunks ─────────────────────────────────────────────────────

    private class Chunk(val firstLine: Int, val text: String, val header: List<String>) {
        /** Annotated `@Preview`, or a fixture named `sample…` / `preview…`. */
        fun isPreviewOnly(): Boolean = header.any { line ->
            line.startsWith("@Preview") || PREVIEW_FIXTURE.containsMatchIn(line)
        }
    }

    /**
     * Splits the file at column-0 declarations (Kotlin style puts every top-level declaration
     * there); annotation lines stick to the declaration below them.
     */
    private fun topLevelChunks(source: String): List<Chunk> {
        val lines = source.lines()
        val chunks = mutableListOf<Chunk>()
        var start = 0
        var header = mutableListOf<String>()
        var inDeclaration = false
        fun flush(end: Int) {
            if (end > start) chunks += Chunk(start, lines.subList(start, end).joinToString("\n"), header)
        }
        lines.forEachIndexed { index, line ->
            if (!TOP_LEVEL_START.containsMatchIn(line)) return@forEachIndexed
            val isAnnotation = line.startsWith("@")
            if (inDeclaration || header.isEmpty()) {
                flush(index)
                start = index
                header = mutableListOf()
                inDeclaration = false
            }
            header += line
            if (!isAnnotation) inDeclaration = true
        }
        flush(lines.size)
        return chunks
    }

    // ── Lexer ────────────────────────────────────────────────────────────────

    /** String literals of [code] with their 0-based line, skipping comments and annotation args. */
    private fun literals(code: String): List<Literal> {
        val found = mutableListOf<Literal>()
        var i = 0
        fun lineAt(pos: Int) = code.substring(0, pos).count { it == '\n' }
        while (i < code.length) {
            when {
                code.startsWith("//", i) -> i = code.indexOf('\n', i).let { if (it < 0) code.length else it }
                code.startsWith("/*", i) -> i = code.indexOf("*/", i + 2).let { if (it < 0) code.length else it + 2 }
                code[i] == '@' && i + 1 < code.length && code[i + 1].isLetter() -> i = skipAnnotation(code, i)
                code.startsWith("\"\"\"", i) -> {
                    val end = code.indexOf("\"\"\"", i + 3).let { if (it < 0) code.length else it }
                    found += Literal(lineAt(i), blankTemplates(code.substring(i + 3, end)))
                    i = end + 3
                }
                code[i] == '"' -> {
                    val (text, end) = readQuoted(code, i)
                    found += Literal(lineAt(i), text)
                    i = end
                }
                code[i] == '\'' -> i = skipCharLiteral(code, i)
                else -> i++
            }
        }
        return found
    }

    /** Reads a `"…"` literal from the opening quote; returns its text and the index after it. */
    private fun readQuoted(code: String, open: Int): Pair<String, Int> {
        val text = StringBuilder()
        var j = open + 1
        while (j < code.length && code[j] != '"' && code[j] != '\n') {
            when {
                code[j] == '\\' -> { text.append(code[j + 1]); j += 2 }
                code.startsWith("\${", j) -> { j = skipBalanced(code, j + 1, '{', '}'); text.append(' ') }
                code[j] == '$' && j + 1 < code.length && code[j + 1].isJavaIdentifierStart() -> {
                    j++
                    while (j < code.length && code[j].isJavaIdentifierPart()) j++
                    text.append(' ')
                }
                else -> { text.append(code[j]); j++ }
            }
        }
        return text.toString() to j + 1
    }

    /** `@Name` or `@Name(…)`, including `@param:Name`; returns the index after it. */
    private fun skipAnnotation(code: String, at: Int): Int {
        var j = at + 1
        while (j < code.length && (code[j].isJavaIdentifierPart() || code[j] == '.' || code[j] == ':')) j++
        return if (j < code.length && code[j] == '(') skipBalanced(code, j, '(', ')') else j
    }

    /** From an [open] bracket at [at], the index after its matching [close], skipping strings. */
    private fun skipBalanced(code: String, at: Int, open: Char, close: Char): Int {
        var depth = 0
        var j = at
        while (j < code.length) {
            when (code[j]) {
                '"' -> { j = readQuoted(code, j).second; continue }
                open -> depth++
                close -> if (--depth == 0) return j + 1
            }
            j++
        }
        return code.length
    }

    private fun skipCharLiteral(code: String, at: Int): Int {
        val end = code.indexOf('\'', if (code.getOrNull(at + 1) == '\\') at + 3 else at + 2)
        return if (end < 0) at + 1 else end + 1
    }

    private fun blankTemplates(raw: String): String =
        raw.replace(Regex("""\$\{[^}]*\}|\$[A-Za-z_]\w*"""), " ")

    private fun isIgnoredLine(source: String, line: Int): Boolean =
        source.lines().getOrNull(line)?.contains(IGNORE_MARKER) == true

    private companion object {
        const val IGNORE_MARKER = "i18n-ignore"
        val SPANISH_ONLY_CHARS = Regex("[áéíóúñüÁÉÍÓÚÑÜ¿¡]")
        val WORD = Regex("""\p{L}+""")
        val WHITESPACE = Regex("""\s+""")

        /**
         * Spanish function words that don't occur as English words, so English text (and log
         * messages, which are English) never trips rule 2. "no", "a" and "es" are left out.
         */
        val FUNCTION_WORDS = setOf(
            "de", "del", "la", "las", "el", "los", "lo", "le", "que", "tu", "tus", "un", "una",
            "unos", "unas", "para", "con", "sin", "por", "se", "al", "y", "mi", "mis", "su", "sus"
        )

        val TOP_LEVEL_START = Regex(
            "^(@|private |internal |public |protected |fun |val |var |class |data |sealed |" +
                "object |enum |abstract |open |const |typealias |interface |annotation )"
        )
        val PREVIEW_FIXTURE = Regex("""\b(fun|val|var)\s+(`)?(sample|preview)\w*""", RegexOption.IGNORE_CASE)
    }
}

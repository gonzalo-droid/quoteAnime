package com.gondroid.quoteanime.resources

import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Reads a `strings.xml` straight from the source tree, for tests that pin rules about the text
 * itself (register, duplicates) rather than about code that uses it.
 *
 * A plain JVM unit test has no Android `Resources` (the project has no Robolectric), and going
 * through `R` would only give ids, not text. Parsing the file is enough: these rules are about
 * what translators wrote, before aapt touches it. Gradle runs unit tests with the module
 * directory (`app/`) as working directory; the `app/` fallback covers running from the root.
 */
object StringResourceFile {

    /** One translatable entry: a `<string>` or one `<item>` of a `<plurals>`/`<string-array>`. */
    data class Entry(val name: String, val text: String)

    fun load(valuesDir: String): List<Entry> {
        val file = locate("src/main/res/$valuesDir/strings.xml")
        val document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)
        val root = document.documentElement
        val entries = mutableListOf<Entry>()
        val children = root.childNodes
        for (i in 0 until children.length) {
            val node = children.item(i) as? Element ?: continue
            val name = node.getAttribute("name")
            when (node.tagName) {
                "string" -> entries += Entry(name, unescape(node.textContent))
                "plurals", "string-array" -> {
                    val items = node.getElementsByTagName("item")
                    for (j in 0 until items.length) {
                        entries += Entry(name, unescape(items.item(j).textContent))
                    }
                }
            }
        }
        return entries
    }

    private fun locate(relativePath: String): File =
        listOf(File(relativePath), File("app/$relativePath"))
            .firstOrNull { it.isFile }
            ?: error("Could not find $relativePath from ${File(".").absolutePath}")

    /** Undoes Android's backslash escapes so `\n` doesn't glue two words together. */
    private fun unescape(raw: String): String =
        raw.replace("\\n", "\n").replace("\\'", "'").replace("\\\"", "\"")
}

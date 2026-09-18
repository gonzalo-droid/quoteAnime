package com.gondroid.quoteanime.presentation.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.w3c.dom.Element
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A widget or notification tap with the app open must reach the running MainActivity through
 * `onNewIntent`, not destroy it and start over from the splash. Two things guarantee it, and
 * both live outside Kotlin logic a unit test could call, so they're read from the sources:
 * the manifest's `launchMode` and the flags of every intent that targets MainActivity.
 *
 * Scenarios covered:
 *  - MainActivity is `singleTop` in the manifest
 *  - Every `Intent(…, MainActivity::class.java)` in the app uses AppDeepLink.LAUNCH_FLAGS and
 *    none uses FLAG_ACTIVITY_CLEAR_TASK (a new widget or notification can't regress it)
 */
class MainActivityLaunchTest {

    @Test
    fun `given the manifest, then MainActivity is singleTop`() {
        val document = DocumentBuilderFactory.newInstance()
            .apply { isNamespaceAware = true }
            .newDocumentBuilder()
            .parse(locate("src/main/AndroidManifest.xml"))
        val activities = document.getElementsByTagName("activity")
        val main = (0 until activities.length)
            .map { activities.item(it) as Element }
            .single { it.getAttributeNS(ANDROID_NS, "name") == ".MainActivity" }

        assertEquals("singleTop", main.getAttributeNS(ANDROID_NS, "launchMode"))
    }

    @Test
    fun `given every intent that opens MainActivity, then it uses the shared launch flags`() {
        val sources = locate("src/main/java").walkTopDown().filter { it.extension == "kt" }
        val intents = sources.flatMap { file ->
            val text = file.readText()
            INTENT.findAll(text).map { match ->
                // The intent's `apply { … }` block: up to the first closing brace after it.
                val block = text.substring(match.range.first, text.indexOf('}', match.range.last).coerceAtLeast(match.range.last))
                "${file.name}:${text.substring(0, match.range.first).count { it == '\n' } + 1}" to block
            }
        }.toList()

        assertTrue("No intent to MainActivity found — the scan is broken", intents.size >= 5)
        val wrong = intents.filter { (_, block) ->
            "AppDeepLink.LAUNCH_FLAGS" !in block || "FLAG_ACTIVITY_CLEAR_TASK" in block
        }.map { it.first }
        assertEquals("Intents to MainActivity without AppDeepLink.LAUNCH_FLAGS", emptyList<String>(), wrong)
    }

    private fun locate(path: String): File =
        listOf(File(path), File("app/$path")).firstOrNull { it.exists() }
            ?: error("Could not find $path from ${File(".").absolutePath}")

    private companion object {
        const val ANDROID_NS = "http://schemas.android.com/apk/res/android"
        val INTENT = Regex("""Intent\(\s*\w+\s*,\s*MainActivity::class\.java\s*\)""")
    }
}

package com.gondroid.quoteanime.resources

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every file in `res/font` must be a real font. Eight of them (Playfair Display and Lora) were
 * once committed as GitHub HTML pages — a "download" of the page instead of the raw file — and
 * aapt packages them without complaint; the app only finds out when Compose tries to load them
 * on the quote cards. Checking the first bytes is enough to catch that.
 */
class FontFilesTest {

    private val fontDir = listOf(File("src/main/res/font"), File("app/src/main/res/font"))
        .first { it.isDirectory }

    /** TrueType (`00 01 00 00`, `true`), OpenType/CFF (`OTTO`) and font collections (`ttcf`). */
    private val fontSignatures = listOf(
        byteArrayOf(0x00, 0x01, 0x00, 0x00),
        "true".toByteArray(),
        "OTTO".toByteArray(),
        "ttcf".toByteArray()
    )

    @Test
    fun `every file in res font starts with a font signature`() {
        val files = fontDir.listFiles { f -> f.isFile && f.extension in setOf("ttf", "otf") }.orEmpty()
        assertTrue("no fonts found in $fontDir", files.isNotEmpty())

        val broken = files.filter { file ->
            val head = file.inputStream().use { it.readNBytes(4) }
            fontSignatures.none { it.contentEquals(head) }
        }.map { it.name }.sorted()

        assertTrue("not real font files (HTML or corrupt?): $broken", broken.isEmpty())
    }
}

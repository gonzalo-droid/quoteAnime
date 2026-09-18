package com.gondroid.quoteanime.resources

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Spanish copy addresses the user as "tú" (the same register as the iOS app). Some strings
 * slipped in with Rioplatense voseo ("Probá", "sos", "Cancelá"); this pins the fix so a new
 * string can't reintroduce it unnoticed.
 *
 * The list is explicit on purpose: a generic "verb ending in a stressed vowel" rule would also
 * flag legitimate futures and preterites ("funcionará", "salió", "Recibirás"). When a new voseo
 * form shows up in review, add it here.
 *
 * Scenarios covered:
 *  - No `values-es` string contains any listed voseo form
 *  - The matcher does catch the forms (a positive control, so an accent/word-boundary bug in the
 *    regex can't make the first test pass vacuously)
 *  - The matcher leaves the tuteo equivalents alone
 */
class SpanishRegisterTest {

    @Test
    fun `given the Spanish strings, when scanned, then none uses voseo`() {
        val offenders = StringResourceFile.load("values-es")
            .mapNotNull { entry -> voseoIn(entry.text)?.let { "${entry.name}: \"$it\" in \"${entry.text}\"" } }

        assertEquals("Voseo found in values-es/strings.xml:\n" + offenders.joinToString("\n"), emptyList<String>(), offenders)
    }

    @Test
    fun `given sentences in voseo, when scanned, then the voseo form is reported`() {
        assertEquals("Probá", voseoIn("Algo salió mal. Probá de nuevo."))
        assertEquals("sos", voseoIn("✨ Ya sos premium"))
        assertEquals("querés", voseoIn("¿Seguro que querés cancelar?"))
        assertEquals("restauralo", voseoIn("pero conserva su historial — restauralo cuando quieras."))
    }

    @Test
    fun `given the same sentences in tuteo, when scanned, then nothing is reported`() {
        listOf(
            "Algo salió mal. Prueba de nuevo.",
            "✨ Ya eres premium",
            "¿Seguro que quieres cancelar?",
            "pero conserva su historial — restáuralo cuando quieras.",
            "Google Play no está disponible ahora. Inténtalo más tarde.",
            "Sé un saiyan"
        ).forEach { assertTrue(it, voseoIn(it) == null) }
    }

    private fun voseoIn(text: String): String? = VOSEO_REGEX.find(text)?.value

    private companion object {
        val VOSEO_FORMS = listOf(
            // Pronoun and present indicative
            "vos", "sos", "podés", "querés", "tenés", "sabés", "hacés", "decís", "venís", "volvés",
            "conservás", "archivás", "cancelás", "perdés", "necesitás", "elegís", "empezás",
            // Imperative
            "elegí", "marcá", "creá", "probá", "revisá", "tocá", "cancelá", "desbloqueá", "navegá",
            "accedé", "intentá", "mirá", "agregá", "añadí", "usá", "activá", "abrí", "esperá",
            "tené", "poné", "hacé", "vení", "andá", "decí", "seguí", "guardá", "compartí", "borrá",
            "archivá", "restaurá", "empezá", "comenzá", "descubrí", "invitá", "dejá", "pintá",
            "configurá", "elegilo", "fijate", "animate", "contanos", "escribinos", "dejanos",
            "restauralo", "activalo", "activalas", "descargala", "pintalos"
        )

        // (?i) case-insensitive, (?U) Unicode-aware \b so "á" counts as a letter.
        val VOSEO_REGEX = Regex("(?iU)\\b(" + VOSEO_FORMS.joinToString("|") + ")\\b")
    }
}

package com.gondroid.quoteanime.domain.model

/**
 * What a template's `title` means: a string-resource key the presentation layer localizes
 * (`template_theme_ninja`, as in [DefaultHabitTemplates] and `/habitTemplates`), or literal
 * text shown as is ("Leer 20 minutos").
 *
 * A key published remotely before this build knows it (`template_theme_bleach`) has no
 * translation, and showing it would put a raw id on a chip. Such a template is dropped
 * instead — a suggestion needs a name to be worth offering, and the release that adds the
 * key brings it back. Keys are told apart from literal titles by shape: lowercase words
 * joined by underscores. Same criterion as the iOS app.
 */
object HabitTemplateTitles {

    sealed interface Title {
        /** A key this build can localize (see `TEMPLATE_TITLE_RES_BY_KEY` in the presentation layer). */
        data class Key(val key: String) : Title

        /** Human text, shown unchanged. */
        data class Literal(val text: String) : Title
    }

    /** Every title key this build can localize. Must match the presentation layer's resource map. */
    val KNOWN_KEYS: Set<String> = setOf(
        "template_train",
        "template_read",
        "template_meditate",
        "template_water",
        "template_sleep_early",
        "template_study",
        "template_write",
        "template_walk",
        "template_theme_ninja",
        "template_theme_one_piece",
        "template_theme_saiyan",
        "template_theme_pokemon",
        "template_theme_black_clover"
    )

    private val KEY_SHAPE = Regex("[a-z0-9]+(_[a-z0-9]+)+")

    /** The title's meaning, or null when it can't be shown: blank, or a key this build doesn't know. */
    fun parse(raw: String): Title? {
        val title = raw.trim()
        return when {
            title.isEmpty() -> null
            title in KNOWN_KEYS -> Title.Key(title)
            looksLikeKey(title) -> null
            else -> Title.Literal(title)
        }
    }

    /** True if [template]'s title can be shown to the user. */
    fun isDisplayable(template: HabitTemplate): Boolean = parse(template.title) != null

    /** `template_theme_bleach`: lowercase words joined by underscores. A one-word lowercase
     *  title ("leer") has no underscore and is taken as literal text. */
    fun looksLikeKey(text: String): Boolean = KEY_SHAPE.matches(text)
}

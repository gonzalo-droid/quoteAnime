package com.gondroid.quoteanime.presentation.navigation

import androidx.compose.runtime.saveable.Saver

/**
 * A destination the app was asked to open from outside: a habit reminder or a Mi Rutina widget
 * ([Routine]), or the quote widget ([Quote]).
 */
sealed interface AppDeepLink {
    data object Routine : AppDeepLink
    data class Quote(val id: String) : AppDeepLink

    companion object {
        /** Intent extra the quote widget uses to ask for a specific quote. */
        const val EXTRA_QUOTE_ID = "widget_quote_id"

        /** Reads the two intent extras that can open a destination; a quote wins over Mi Rutina. */
        fun from(quoteId: String?, openRoutine: Boolean): AppDeepLink? = when {
            quoteId != null -> Quote(quoteId)
            openRoutine -> Routine
            else -> null
        }
    }
}

/**
 * The back stack, bottom first, to show once the app reaches its main content with [this]
 * link pending. Home always sits at the bottom so "back" from the destination returns to it
 * instead of closing the app; the quote link is Home itself, positioned on that quote.
 */
fun AppDeepLink?.mainBackStack(): List<String> = when (this) {
    null -> listOf(Screen.Home.route)
    AppDeepLink.Routine -> listOf(Screen.Home.route, Screen.Routine.route)
    is AppDeepLink.Quote -> listOf(Screen.Home.createRoute(id))
}

/**
 * Holds a deep link until there is a main back stack to apply it to.
 *
 * A link can arrive before that exists — a cold start from a reminder or a widget (the splash
 * is still deciding) or a tap while the onboarding is on screen. It is parked here and applied
 * by [enterMain] when the splash routes to Home or the onboarding finishes: never by skipping
 * an onboarding that isn't complete. Once the app is in its main content, [open] applies the
 * link right away.
 */
class DeepLinkRouter(pending: AppDeepLink? = null) {

    var pending: AppDeepLink? = pending
        private set

    /** The splash (onboarding complete) or the onboarding finished: routes to show, consuming the pending link. */
    fun enterMain(): List<String> {
        val link = pending
        pending = null
        return link.mainBackStack()
    }

    /**
     * A link that arrived while the app is running. Returns the back stack to show now, or
     * null when [isMainReady] is false and the link was parked for [enterMain].
     */
    fun open(link: AppDeepLink, isMainReady: Boolean): List<String>? {
        if (!isMainReady) {
            pending = link
            return null
        }
        pending = null
        return link.mainBackStack()
    }

    companion object {
        private const val ROUTINE_TOKEN = "routine"
        private const val QUOTE_PREFIX = "quote:"

        /** Keeps a parked link across configuration changes and process death. */
        val Saver: Saver<DeepLinkRouter, String> = Saver(
            save = { router ->
                when (val link = router.pending) {
                    null -> ""
                    AppDeepLink.Routine -> ROUTINE_TOKEN
                    is AppDeepLink.Quote -> QUOTE_PREFIX + link.id
                }
            },
            restore = { token ->
                DeepLinkRouter(
                    when {
                        token == ROUTINE_TOKEN -> AppDeepLink.Routine
                        token.startsWith(QUOTE_PREFIX) -> AppDeepLink.Quote(token.removePrefix(QUOTE_PREFIX))
                        else -> null
                    }
                )
            }
        )
    }
}

package com.gondroid.quoteanime.presentation.navigation

import android.content.Intent
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

        /**
         * Flags for every intent that opens [com.gondroid.quoteanime.MainActivity] from outside
         * (widgets, notifications). `CLEAR_TOP` alone — with the activity in the default
         * `standard` mode — finishes the running instance and creates a new one: the app
         * restarted from the splash on every tap. With `SINGLE_TOP` (and `launchMode="singleTop"`
         * in the manifest, for any caller that forgets it) the running activity is reused and
         * the link arrives through `onNewIntent`, where [DeepLinkRouter] applies it.
         * Never `CLEAR_TASK`: it destroys the task and whatever the user had open.
         */
        const val LAUNCH_FLAGS = Intent.FLAG_ACTIVITY_NEW_TASK or
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
            Intent.FLAG_ACTIVITY_SINGLE_TOP

        /**
         * The link an intent that started the activity asks for, from its extras and flags.
         *
         * Reopening the app from Recents re-delivers the intent that first created the task,
         * extras included, flagged `FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY`. Honouring it would
         * repeat an old widget or reminder tap — Mi Rutina or a quote the user left long ago —
         * so such an intent carries no link.
         */
        fun fromLaunch(quoteId: String?, openRoutine: Boolean, intentFlags: Int): AppDeepLink? =
            if ((intentFlags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0) null
            else from(quoteId, openRoutine)

        /**
         * Reads the two intent extras that can open a destination; a quote wins over Mi Rutina.
         * A blank quote id counts as no quote: the widget has none while loading or after an
         * error, and navigating to `home?quoteId=` with it would clear the back stack.
         */
        fun from(quoteId: String?, openRoutine: Boolean): AppDeepLink? = when {
            !quoteId.isNullOrBlank() -> Quote(quoteId)
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
                        token.startsWith(QUOTE_PREFIX) -> AppDeepLink.from(
                            quoteId = token.removePrefix(QUOTE_PREFIX),
                            openRoutine = false
                        )
                        else -> null
                    }
                )
            }
        )
    }
}

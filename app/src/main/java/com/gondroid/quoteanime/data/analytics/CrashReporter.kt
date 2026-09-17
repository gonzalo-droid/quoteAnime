package com.gondroid.quoteanime.data.analytics

import com.google.firebase.crashlytics.FirebaseCrashlytics
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Non-fatal reporting. Crashlytics was already in the build but only ever reported what it
 * collects on its own — crashes. The failures that cost this app money aren't crashes: a
 * purchase that Play rejects, an acknowledgement that never lands, an entitlement query that
 * fails. Those used to leave no trace at all, which made "no me deja pagar" unanswerable.
 */
interface CrashReporter {
    fun recordNonFatal(error: Throwable, context: Map<String, String> = emptyMap())
}

/** Thrown nowhere — it exists to give Crashlytics a typed, groupable non-fatal. */
class BillingFailure(message: String) : Exception(message)

@Singleton
class FirebaseCrashReporter @Inject constructor() : CrashReporter {

    override fun recordNonFatal(error: Throwable, context: Map<String, String>) {
        val crashlytics = FirebaseCrashlytics.getInstance()
        context.forEach { (key, value) -> crashlytics.setCustomKey(key, value) }
        crashlytics.recordException(error)
    }
}

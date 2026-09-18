package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.domain.model.validAnimeSelection
import com.gondroid.quoteanime.domain.repository.UserPreferencesRepository
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * Drops the saved anime selection's values that no longer name an existing anime, and saves the
 * cleaned selection — see [validAnimeSelection]. Needed because a build listed emotions under
 * "Animes": a user who picked "motivación" there must get every anime back, not an empty feed.
 *
 * Returns the selection to apply. It is returned even if saving fails; the next run retries.
 */
class ReconcileAnimeSelectionUseCase @Inject constructor(
    private val repository: UserPreferencesRepository
) {
    suspend operator fun invoke(saved: Set<String>, available: Collection<String>): Set<String> {
        val valid = validAnimeSelection(saved, available)
        if (valid != saved) {
            try {
                repository.updateSelectedCategories(valid)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Applying the valid selection matters more than persisting it right now.
            }
        }
        return valid
    }
}

package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.data.remote.HabitTemplateRemoteDataSource
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.HabitTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import javax.inject.Inject

/**
 * Remote templates let new habit suggestions ship without an app release.
 * The bundled list keeps the editor usable offline and on first launch.
 *
 * The bundled list is emitted **immediately** and replaced when `/habitTemplates` answers.
 * Waiting for the remote first isn't an option: without network (and the RTDB has no disk
 * persistence here) its listener never fires at all, which left the editor and the
 * onboarding without suggestions. A remote failure keeps whatever was last shown — the
 * bundled list, or an earlier remote one — rather than emptying the list.
 */
class GetHabitTemplatesUseCase @Inject constructor(
    private val remoteDataSource: HabitTemplateRemoteDataSource
) {
    operator fun invoke(): Flow<List<HabitTemplate>> =
        remoteDataSource.getTemplates()
            .map { templates ->
                if (templates.isEmpty()) DefaultHabitTemplates.ALL
                else templates.sortedBy { it.order }
            }
            .onStart { emit(DefaultHabitTemplates.ALL) }
            .catch { /* keep the last emitted list */ }
            .distinctUntilChanged()
}

package com.gondroid.quoteanime.domain.usecase

import com.gondroid.quoteanime.data.remote.HabitTemplateRemoteDataSource
import com.gondroid.quoteanime.domain.model.DefaultHabitTemplates
import com.gondroid.quoteanime.domain.model.HabitTemplate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Remote templates let new habit suggestions ship without an app release.
 * The bundled list keeps the editor usable offline and on first launch.
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
            .catch { emit(DefaultHabitTemplates.ALL) }
}

package com.gondroid.quoteanime.data.remote

import com.gondroid.quoteanime.data.remote.dto.toDomain
import com.gondroid.quoteanime.data.remote.dto.toHabitTemplateDto
import com.gondroid.quoteanime.domain.model.HabitTemplate
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

class HabitTemplateRemoteDataSource @Inject constructor(
    private val database: FirebaseDatabase
) {
    private val templatesRef = database.getReference("habitTemplates")

    fun getTemplates(): Flow<List<HabitTemplate>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val templates = snapshot.children
                    .mapNotNull { it.toHabitTemplateDto()?.toDomain() }
                trySend(templates)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }
        templatesRef.addValueEventListener(listener)
        awaitClose { templatesRef.removeEventListener(listener) }
    }
}

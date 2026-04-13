package com.gondroid.quoteanime.domain.repository

import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    fun getCategories(): Flow<List<Category>>
    fun getAllQuotes(): Flow<List<Quote>>
    fun getQuotesByCategory(categoryId: String): Flow<List<Quote>>
    fun getFavorites(): Flow<List<Quote>>
    fun isFavorite(quoteId: String): Flow<Boolean>
    suspend fun getRandomQuote(categoryIds: Set<String>, excludeId: String? = null): Quote?
    suspend fun addFavorite(quote: Quote)
    suspend fun removeFavorite(quoteId: String)
}

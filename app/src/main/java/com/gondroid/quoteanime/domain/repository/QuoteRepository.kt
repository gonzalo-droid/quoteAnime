package com.gondroid.quoteanime.domain.repository

import com.gondroid.quoteanime.domain.model.Quote
import kotlinx.coroutines.flow.Flow

interface QuoteRepository {
    /** Distinct anime names (`Quote.anime`), sorted — the Settings anime selector. */
    fun getAnimes(): Flow<List<String>>
    fun getAllQuotes(): Flow<List<Quote>>
    /** Quotes tagged with the emotion [categoryId] (`Quote.categories`) — the Catalogue filter. */
    fun getQuotesByCategory(categoryId: String): Flow<List<Quote>>
    fun getFavorites(): Flow<List<Quote>>
    fun isFavorite(quoteId: String): Flow<Boolean>
    /** A random quote of the anime selection [animes] (empty = every anime), for notifications and the widget. */
    suspend fun getRandomQuote(animes: Set<String>, excludeId: String? = null): Quote?
    suspend fun addFavorite(quote: Quote)
    suspend fun removeFavorite(quoteId: String)
}

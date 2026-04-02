package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.entity.toDomain
import com.gondroid.quoteanime.data.local.db.entity.toFavoriteEntity
import com.gondroid.quoteanime.data.remote.QuoteRemoteDataSource
import com.gondroid.quoteanime.data.remote.dto.toDomain
import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: QuoteRemoteDataSource,
    private val favoriteQuoteDao: FavoriteQuoteDao
) : QuoteRepository {

    override fun getCategories(): Flow<List<Category>> =
        remoteDataSource.getCategories().map { dtos -> dtos.map { it.toDomain() } }

    override fun getAllQuotes(): Flow<List<Quote>> =
        combine(
            remoteDataSource.getAllQuotes(),
            favoriteQuoteDao.getFavoriteIds()
        ) { dtos, favoriteIds ->
            dtos.map { it.toDomain(isFavorite = it.id in favoriteIds) }
        }

    override fun getQuotesByCategory(categoryId: String): Flow<List<Quote>> =
        combine(
            remoteDataSource.getQuotesByCategory(categoryId),
            favoriteQuoteDao.getFavoriteIds()
        ) { dtos, favoriteIds ->
            dtos.map { it.toDomain(isFavorite = it.id in favoriteIds) }
        }

    override fun getFavorites(): Flow<List<Quote>> =
        favoriteQuoteDao.getFavorites().map { entities -> entities.map { it.toDomain() } }

    override fun isFavorite(quoteId: String): Flow<Boolean> =
        favoriteQuoteDao.isFavorite(quoteId)

    override suspend fun getRandomQuote(categoryIds: Set<String>): Quote? =
        remoteDataSource.getRandomQuote(categoryIds)?.toDomain()

    override suspend fun addFavorite(quote: Quote) =
        favoriteQuoteDao.insert(quote.toFavoriteEntity())

    override suspend fun removeFavorite(quoteId: String) =
        favoriteQuoteDao.delete(quoteId)
}

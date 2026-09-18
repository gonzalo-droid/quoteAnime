package com.gondroid.quoteanime.data.repository

import com.gondroid.quoteanime.data.local.db.dao.FavoriteQuoteDao
import com.gondroid.quoteanime.data.local.db.entity.toDomain
import com.gondroid.quoteanime.data.local.db.entity.toFavoriteEntity
import com.gondroid.quoteanime.data.remote.QuoteRemoteDataSource
import com.gondroid.quoteanime.data.remote.dto.toDomain
import com.gondroid.quoteanime.domain.model.Quote
import com.gondroid.quoteanime.domain.model.animeNamesOf
import com.gondroid.quoteanime.domain.model.hasEmotion
import com.gondroid.quoteanime.domain.model.pickRandomFromAnimes
import com.gondroid.quoteanime.domain.repository.QuoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class QuoteRepositoryImpl @Inject constructor(
    private val remoteDataSource: QuoteRemoteDataSource,
    private val favoriteQuoteDao: FavoriteQuoteDao
) : QuoteRepository {

    // Fetched once per app session; maps animeSlug → list of image URLs.
    @Volatile private var imagesCache: Map<String, List<String>>? = null

    // Stable random pick per slug within a session so images don't change
    // when favorites are toggled or the list is reloaded.
    private val resolvedImages = mutableMapOf<String, String?>()

    private suspend fun getImagesMap(): Map<String, List<String>> =
        imagesCache ?: remoteDataSource.getAnimeImages().also { imagesCache = it }

    private suspend fun resolveImageUrl(slug: String?): String? {
        if (slug == null) return null
        val map = getImagesMap()
        return synchronized(resolvedImages) {
            resolvedImages.getOrPut(slug) { map[slug]?.randomOrNull() }
        }
    }

    override fun getAnimes(): Flow<List<String>> =
        remoteDataSource.getAllQuotes().map { dtos -> animeNamesOf(dtos.map { it.anime }) }

    override fun getAllQuotes(): Flow<List<Quote>> =
        combine(
            remoteDataSource.getAllQuotes(),
            favoriteQuoteDao.getFavoriteIds()
        ) { dtos, favoriteIds ->
            dtos.map { dto ->
                dto.toDomain(
                    isFavorite = dto.id in favoriteIds,
                    imageUrl = resolveImageUrl(dto.animeSlug)
                )
            }
        }

    override fun getQuotesByCategory(categoryId: String): Flow<List<Quote>> =
        getAllQuotes().map { quotes -> quotes.filter { it.hasEmotion(categoryId) } }

    override fun getFavorites(): Flow<List<Quote>> =
        favoriteQuoteDao.getFavorites().map { entities ->
            entities.map { entity ->
                entity.toDomain(imageUrl = resolveImageUrl(entity.animeSlug))
            }
        }

    override fun isFavorite(quoteId: String): Flow<Boolean> =
        favoriteQuoteDao.isFavorite(quoteId)

    override suspend fun getRandomQuote(animes: Set<String>, excludeId: String?): Quote? {
        val quote = remoteDataSource.getAllQuotesOnce()
            .map { it.toDomain() }
            .pickRandomFromAnimes(animes, excludeId)
            ?: return null
        return quote.copy(imageUrl = resolveImageUrl(quote.animeSlug))
    }

    override suspend fun addFavorite(quote: Quote) =
        favoriteQuoteDao.insert(quote.toFavoriteEntity())

    override suspend fun removeFavorite(quoteId: String) =
        favoriteQuoteDao.delete(quoteId)
}

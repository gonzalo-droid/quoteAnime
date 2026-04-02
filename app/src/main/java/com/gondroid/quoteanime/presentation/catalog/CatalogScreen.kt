package com.gondroid.quoteanime.presentation.catalog

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gondroid.quoteanime.domain.model.Category
import com.gondroid.quoteanime.presentation.components.QuoteCard

@Composable
fun CatalogScreen(
    onNavigateBack: () -> Unit,
    viewModel: CatalogViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { CatalogTopBar(onNavigateBack = onNavigateBack) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            CategoryFilterRow(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onFavoritesSelected = { viewModel.onCategorySelected(null) },
                onCategorySelected = { viewModel.onCategorySelected(it) }
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outline)

            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }

                uiState.isEmpty -> {
                    EmptyState(isFavoritesTab = uiState.isFavoritesTab)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = uiState.quotes,
                            key = { it.id }
                        ) { quote ->
                            QuoteCard(
                                quote = quote,
                                onToggleFavorite = { viewModel.onToggleFavorite(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CatalogTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = "Catálogo",
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Volver",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.background
        )
    )
}

@Composable
private fun CategoryFilterRow(
    categories: List<Category>,
    selectedCategoryId: String?,
    onFavoritesSelected: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FilterChip(
            selected = selectedCategoryId == null,
            onClick = onFavoritesSelected,
            label = { Text("Favoritos") },
            leadingIcon = if (selectedCategoryId == null) {
                {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            } else null
        )

        categories.forEach { category ->
            FilterChip(
                selected = category.id == selectedCategoryId,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.name) }
            )
        }
    }
}

@Composable
private fun EmptyState(isFavoritesTab: Boolean) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Text(
                text = if (isFavoritesTab) "Aún no tienes favoritos"
                       else "No hay frases en esta categoría",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isFavoritesTab)
                    "Toca el corazón en cualquier frase para guardarla aquí."
                else
                    "Vuelve más tarde o elige otra categoría.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

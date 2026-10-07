package com.pemmob.gameexplore.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemmob.gameexplore.data.model.Game
import com.pemmob.gameexplore.data.repository.GameRepository
import kotlinx.coroutines.launch

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val games: List<Game>) : HomeUiState
    data class Error(val message: String) : HomeUiState
}

sealed interface DetailUiState {
    object Loading : DetailUiState
    data class Success(val game: Game) : DetailUiState
    data class Error(val message: String) : DetailUiState
}

class GameViewModel : ViewModel() {
    private val repository = GameRepository()

    var homeUiState: HomeUiState by mutableStateOf(HomeUiState.Loading)
        private set

    var detailUiState: DetailUiState by mutableStateOf(DetailUiState.Loading)
        private set

    var searchQuery: String by mutableStateOf("")
        private set

    init {
        fetchGames()
    }

    fun updateSearchQuery(query: String) {
        searchQuery = query
        fetchGames(query)
    }

    fun fetchGames(query: String? = null) {
        viewModelScope.launch {
            homeUiState = HomeUiState.Loading
            try {
                val searchQueryParam = if (query.isNullOrBlank()) null else query
                val games = repository.getGames(search = searchQueryParam)
                homeUiState = HomeUiState.Success(games)
            } catch (e: Exception) {
                homeUiState = HomeUiState.Error(e.localizedMessage ?: "Terjadi kesalahan saat memuat data")
            }
        }
    }

    fun fetchGameDetail(gameId: Int) {
        viewModelScope.launch {
            detailUiState = DetailUiState.Loading
            try {
                val game = repository.getGameDetail(gameId)
                detailUiState = DetailUiState.Success(game)
            } catch (e: Exception) {
                detailUiState = DetailUiState.Error(e.localizedMessage ?: "Terjadi kesalahan saat memuat detail game")
            }
        }
    }
}

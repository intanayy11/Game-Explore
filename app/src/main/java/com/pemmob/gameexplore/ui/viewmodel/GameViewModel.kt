package com.pemmob.gameexplore.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pemmob.gameexplore.data.model.Game
import com.pemmob.gameexplore.data.repository.GameRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * State-driven UI: satu sealed interface untuk tiap layar, sehingga Compose hanya
 * melakukan recomposition ketika state benar-benar berubah (Loading -> Success/Error).
 */
sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(
        val games: List<Game>,
        val hasMore: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : HomeUiState
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

    /** Job debounce search; dibatalkan tiap kali user mengetik karakter baru. */
        private var searchJob: Job? = null

        /** Halaman berikutnya untuk infinite scroll; di-reset tiap kali search/query baru. */
        private var currentPage = 1

        /** Mencegah request pagination ganda ketika user scroll cepat. */
        private var isLoadingMore = false

    /**
     * Penanda request terakhir. Karena tiap request berjalan di coroutine terpisah,
     * respons lama bisa sampai SETELAH respons baru; dengan id ini respons yang sudah
     * tidak relevan diabaikan sehingga hasil search tidak pernah tertukar.
     */
    private var homeRequestId = 0
    private var detailRequestId = 0

    init {
        fetchGames()
    }

    /**
     * Dipanggil tiap karakter diketik. State searchQuery langsung berubah sehingga
     * recomposition terjadi seketika, tetapi request ke API ditunda 300 ms (debounce)
     * supaya tidak memanggil endpoint berlebihan dan tidak terjadi race condition.
     */
    fun updateSearchQuery(query: String) {
        searchQuery = query
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(SEARCH_DEBOUNCE_MS)
            fetchGames(query)
        }
    }

    /** Memuat daftar game; dipanggil dari init, hasil debounce search, dan tombol "Coba Lagi". */
        fun fetchGames(query: String? = null) {
            val requestId = ++homeRequestId
            currentPage = 1
            isLoadingMore = false
            val keyword = query?.trim().orEmpty().ifEmpty { null }
            viewModelScope.launch {
                homeUiState = HomeUiState.Loading
                try {
                    val response = repository.getGames(search = keyword, page = 1)
                    if (requestId == homeRequestId) {
                        homeUiState = HomeUiState.Success(
                            games = response.results ?: emptyList(),
                            hasMore = response.next != null
                        )
                    }
                } catch (e: Exception) {
                    if (requestId == homeRequestId) {
                        homeUiState = HomeUiState.Error(
                            e.localizedMessage ?: "Terjadi kesalahan saat memuat data"
                        )
                    }
                }
            }
        }

        /** Infinite scroll: memuat halaman berikutnya saat grid sudah sampai bawah. */
            fun loadMoreGames() {
                val state = homeUiState
            if (state !is HomeUiState.Success || !state.hasMore || isLoadingMore) return
            isLoadingMore = true
            val requestId = homeRequestId
            val page = currentPage + 1
            val keyword = searchQuery.trim().ifEmpty { null }
            viewModelScope.launch {
                try {
                    val response = repository.getGames(search = keyword, page = page)
                    if (requestId == homeRequestId && state === homeUiState) {
                        currentPage = page
                        homeUiState = state.copy(
                            games = state.games + (response.results ?: emptyList()),
                            hasMore = response.next != null,
                            isLoadingMore = false
                        )
                    }
                } catch (e: Exception) {
                    if (requestId == homeRequestId) {
                        homeUiState = state.copy(isLoadingMore = false)
                    }
                } finally {
                    isLoadingMore = false
                }
            }
        }

    /**
     * Dipanggil tepat sebelum navigasi ke layar detail: mengosongkan state detail lama
     * supaya frame pertama menampilkan loading, bukan data game yang sebelumnya dibuka.
     */
    fun resetDetailState() {
        detailUiState = DetailUiState.Loading
    }

    fun fetchGameDetail(gameId: Int) {
        val requestId = ++detailRequestId
        detailUiState = DetailUiState.Loading
        viewModelScope.launch {
            try {
                val game = repository.getGameDetail(gameId)
                if (requestId == detailRequestId) {
                    detailUiState = DetailUiState.Success(game)
                }
            } catch (e: Exception) {
                if (requestId == detailRequestId) {
                    detailUiState = DetailUiState.Error(
                        e.localizedMessage ?: "Terjadi kesalahan saat memuat detail game"
                    )
                }
            }
        }
    }

    companion object {
            private const val SEARCH_DEBOUNCE_MS = 300L
            private const val PAGE_SIZE = 20
        }
}

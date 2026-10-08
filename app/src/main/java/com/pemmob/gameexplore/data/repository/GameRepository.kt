package com.pemmob.gameexplore.data.repository

import com.pemmob.gameexplore.data.model.Game
import com.pemmob.gameexplore.data.model.GameResponse
import com.pemmob.gameexplore.data.remote.RetrofitClient
import com.pemmob.gameexplore.util.Constants

class GameRepository {
    private val api = RetrofitClient.apiService

    suspend fun getGames(search: String? = null, page: Int? = null): GameResponse {
        return api.getGames(apiKey = Constants.API_KEY, search = search, page = page)
    }

    suspend fun getGameDetail(gameId: Int): Game {
        return api.getGameDetail(gameId = gameId, apiKey = Constants.API_KEY)
    }
}

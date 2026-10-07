package com.pemmob.gameexplore.data.repository

import com.pemmob.gameexplore.data.model.Game
import com.pemmob.gameexplore.data.remote.RetrofitClient
import com.pemmob.gameexplore.util.Constants

class GameRepository {
    private val api = RetrofitClient.apiService

    suspend fun getGames(search: String? = null): List<Game> {
        val response = api.getGames(apiKey = Constants.API_KEY, search = search)
        return response.results ?: emptyList()
    }

    suspend fun getGameDetail(gameId: Int): Game {
        return api.getGameDetail(gameId = gameId, apiKey = Constants.API_KEY)
    }
}

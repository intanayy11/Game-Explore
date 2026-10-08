package com.pemmob.gameexplore.data.remote

import com.pemmob.gameexplore.data.model.Game
import com.pemmob.gameexplore.data.model.GameResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface RawgApiService {
    @GET("games")
        suspend fun getGames(
            @Query("key") apiKey: String,
            @Query("search") search: String? = null,
            @Query("page") page: Int? = null
        ): GameResponse

    @GET("games/{id}")
    suspend fun getGameDetail(
        @Path("id") gameId: Int,
        @Query("key") apiKey: String
    ): Game
}

package com.pemmob.gameexplore.data.model

import com.google.gson.annotations.SerializedName

data class GameResponse(
    @SerializedName("results")
    val results: List<Game>?,
    @SerializedName("count")
    val count: Int?,
    @SerializedName("next")
    val next: String?
)

data class Game(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String?,
    @SerializedName("rating")
    val rating: Double?,
    @SerializedName("released")
    val released: String?,
    @SerializedName("background_image")
    val backgroundImage: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("description_raw")
    val descriptionRaw: String?,
    @SerializedName("genres")
    val genres: List<Genre>?
)

data class Genre(
    @SerializedName("id")
    val id: Int,
    @SerializedName("name")
    val name: String?
)

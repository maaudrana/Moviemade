package org.michaelbel.data.remote.model

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class CreditsResponse(
        @SerializedName("id") val id: Int,
        @SerializedName("cast") val cast: List<Cast>,
        @SerializedName("crew") val crew: List<Crew>
): Serializable {

    constructor(): this(
            id = 0,
            cast = emptyList(),
            crew = emptyList()
    )
}
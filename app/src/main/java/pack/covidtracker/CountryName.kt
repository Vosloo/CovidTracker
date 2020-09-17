package pack.covidtracker

import com.google.gson.annotations.SerializedName

data class CountryName(
    @SerializedName("Country") val name: String,
    @SerializedName("Slug") val slug: String
)
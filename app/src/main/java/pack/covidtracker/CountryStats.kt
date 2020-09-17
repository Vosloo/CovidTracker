package pack.covidtracker

import com.google.gson.annotations.SerializedName
import java.util.*

data class CountryStats(
    @SerializedName("Country") val country: String,
    @SerializedName("Confirmed") val confirmed: Int,
    @SerializedName("Deaths") val deaths: Int,
    @SerializedName("Recovered") val recovered: Int,
    @SerializedName("Active") val active: Int,
    @SerializedName("Date") val dateChecked: Date
)
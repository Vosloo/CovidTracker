package pack.covidtracker

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path

interface CovidService {
    @GET("countries")
    fun getCountries(): Call<List<CountryName>>

    @GET("total/country/{country}")
    fun getCountryStats(@Path("country") country: String?):
            Call<List<CountryStats>>
}
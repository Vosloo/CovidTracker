package pack.covidtracker

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.google.gson.GsonBuilder
import com.robinhood.ticker.TickerUtils
import kotlinx.android.synthetic.main.activity_main.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

private const val BASE_URL = "https://api.covid19api.com/"
private const val TAG = "MainActivity"
private const val DEFAULT_COUNTRY = "Poland"

class MainActivity : AppCompatActivity() {
    private lateinit var currentlyShowedData: List<CountryStats>
    private lateinit var adapter: MySparkAdapter
    private lateinit var countryNames: Map<String, List<CountryName>>
    private lateinit var covidService: CovidService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportActionBar?.title = getString(R.string.app_description)

        val gson = GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss")
            .create()
        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
        covidService = retrofit.create(CovidService::class.java)

        // Specific country data; Defaults to DEFAULT_COUNTRY
        covidService.getCountryStats(DEFAULT_COUNTRY)
            .enqueue(object: Callback<List<CountryStats>> {
                override fun onFailure(
                    call: Call<List<CountryStats>>,
                    t: Throwable
                ) {
                    Log.e(TAG, "onFailure $t")
                }

                override fun onResponse(
                    call: Call<List<CountryStats>>,
                    response: Response<List<CountryStats>>
                ) {
                    Log.i(TAG, "onResponse $response")

                    val countryStats = response.body()
                    if (countryStats == null || countryStats.isEmpty()) {
                        dataMissing(DEFAULT_COUNTRY)
                        Log.w(TAG, "Invalid response body")
                        return
                    }

                    setupEventListeners()
                    // reversed to start from the oldest data
                    Log.i(TAG, "Updating graph with specific country data")

                    updateDisplay(countryStats)
                }
            })


        // Country names
        covidService.getCountries()
            .enqueue(object: Callback<List<CountryName>> {
                override fun onFailure(
                    call: Call<List<CountryName>>,
                    t: Throwable
                ) {
                    Log.e(TAG, "onFailure $t")
                }

                override fun onResponse(
                    call: Call<List<CountryName>>,
                    response: Response<List<CountryName>>
                ) {
                    Log.i(TAG, "onResponse $response")

                    val countries = response.body()
                    if (countries == null) {
                        Log.w(TAG, "Invalid response body")
                        return
                    }

                    // reversed to start from the oldest data
                    countryNames = countries.reversed()
                        .groupBy { it.name }
                    Log.i(TAG, "Updating spinner with country names")
                    // Update spinner
                    updateSpinner(countryNames.keys)
                }
            })
    }

    private fun dataMissing(selectedCountry: String) {
        // Popup dialog that the data for selected country is missing
        AlertDialog.Builder(this@MainActivity)
            .setTitle("Data missing!")
            .setMessage(
                "It seems that the data for \"$selectedCountry\" is missing.\n"
            )
            .setNeutralButton("Ok", null)
            .show()

        // Create new entries and remove entry from countryNames
        val newEntries = countryNames.keys.toMutableList().apply {
            sort()
            remove(selectedCountry)
        }
        countryNames = countryNames.toMutableMap().apply {
            remove(selectedCountry)
            toMap()
        }

        // Attach new data source
        spinnerSelect.attachDataSource(newEntries)

        // Set labels on last correct data shown
        val defaultIndex = newEntries.indexOf(currentlyShowedData.first().country)
        spinnerSelect.selectedIndex = defaultIndex
        updateDisplay(currentlyShowedData)
    }

    private fun updateSpinner(countries: Set<String>) {
        val countriesList = countries.toMutableList()
        countriesList.sort()

        // Add country list as data source for the spinner
        spinnerSelect.attachDataSource(countriesList)

        // Set default value as DEFAULT_COUNTRY
        spinnerSelect.selectedIndex = countriesList.indexOf(DEFAULT_COUNTRY)

        // Set listener
        spinnerSelect.setOnSpinnerItemSelectedListener { parent, _, position, _ ->
            val selectedCountry = parent.getItemAtPosition(position) as String
            val countrySlug = countryNames[selectedCountry]?.first()?.slug
            covidService.getCountryStats(countrySlug)
                .enqueue(object: Callback<List<CountryStats>> {
                    override fun onFailure(
                        call: Call<List<CountryStats>>,
                        t: Throwable
                    ) {
                        Log.e(TAG, "onFailure $t")
                    }

                    override fun onResponse(
                        call: Call<List<CountryStats>>,
                        response: Response<List<CountryStats>>
                    ) {
                        Log.i(TAG, "onResponse $response")

                        val countryStats = response.body()
                        if (countryStats == null || countryStats.isEmpty()) {
                            Log.w(TAG, "Invalid response body")
                            dataMissing(selectedCountry)
                            return
                        }

                        setupEventListeners()
                        // reversed to start from the oldest data
                        Log.i(TAG, "Updating graph with national data")

                        updateDisplay(countryStats)
                    }
                })
        }
    }

    private fun setupEventListeners() {
        valueTicker.setCharacterLists(TickerUtils.provideNumberList())

        // Listener for selecting data on chart
        sparkView.isScrubEnabled = true
        sparkView.setScrubListener { itemData ->
            if (itemData is CountryStats) {
                updateDate(itemData)
            }
        }

        // Listener for radio buttons
        timeSelection.setOnCheckedChangeListener { _, checkedId ->
            adapter.daysAgo = when(checkedId) {
                R.id.radioWeek -> TimeScale.WEEK
                R.id.radioMonth -> TimeScale.MONTH
                else -> TimeScale.ALL
            }
            adapter.notifyDataSetChanged()
        }

        valueSelection.setOnCheckedChangeListener { _, checkedId ->
            when(checkedId) {
                R.id.radioConfirmed -> updateValue(Value.CONFIRMED)
                R.id.radioRecovered -> updateValue(Value.RECOVERED)
                R.id.radioDeath -> updateValue(Value.DEATH)
                R.id.radioActive -> updateValue(Value.ACTIVE)
            }
        }
    }

    private fun updateValue(value: Value) {
        // Update color of the chart
        val color = when (value) {
            Value.RECOVERED -> R.color.colorRecovered
            Value.CONFIRMED -> R.color.colorConfirmed
            Value.DEATH -> R.color.colorDeath
            Value.ACTIVE -> R.color.colorActive
        }
        val colorInt = ContextCompat.getColor(this, color)
        sparkView.lineColor = colorInt
        valueTicker.textColor = colorInt

        // Update value on adapter
        adapter.value = value

        // Update bounds on adapter
        adapter.dataBounds
        adapter.notifyDataSetChanged()

        // Reset value and day on radio buttons change
        updateDate(currentlyShowedData.last())
    }

    private fun updateDisplay(dailyData: List<CountryStats>) {
        currentlyShowedData = dailyData
        // Create a new SparkAdapter with data
        adapter = MySparkAdapter(dailyData)
        sparkView.adapter = adapter
        // Update radio buttons to select the positive vases and all time by default
        radioConfirmed.isChecked = true
        radioAll.isChecked = true
        // Display value for the most recent date
        updateValue(Value.CONFIRMED)
    }

    private fun updateDate(stats: CountryStats) {
        val numCases = when (adapter.value) {
            Value.RECOVERED -> stats.recovered
            Value.CONFIRMED -> stats.confirmed
            Value.DEATH -> stats.deaths
            Value.ACTIVE -> stats.active
        }

        valueTicker.text = NumberFormat.getInstance()
            .format(numCases)

        val outputDate = SimpleDateFormat("MMM dd, yyyy", Locale.US)
        dateLabel.text = outputDate.format(stats.dateChecked)
    }
}
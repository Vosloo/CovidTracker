package pack.covidtracker

import android.graphics.RectF
import android.util.Log
import com.robinhood.spark.SparkAdapter

class MySparkAdapter(private val dailyData: List<CountryStats>) : SparkAdapter() {

    var value = Value.CONFIRMED
    var daysAgo = TimeScale.ALL

    var curMin: Float = 0.toFloat()

    override fun getCount(): Int {
        return dailyData.size
    }

    override fun getItem(index: Int): Any {
        return dailyData[index]
    }

    override fun getY(index: Int): Float {
        val chosenDayData = dailyData[index]

        return when(value) {
            Value.RECOVERED -> chosenDayData.recovered.toFloat()
            Value.CONFIRMED -> chosenDayData.confirmed.toFloat()
            Value.DEATH -> chosenDayData.deaths.toFloat()
            Value.ACTIVE -> chosenDayData.active.toFloat()
        }
    }

    override fun hasBaseLine(): Boolean {
        return true
    }

    override fun getBaseLine(): Float {
        return curMin
    }

    override fun getDataBounds(): RectF {
        // Moving left bound to see more / less days
        // Moving top bound to scale the data on the entire sparkView
        val bounds = super.getDataBounds()

        if (daysAgo != TimeScale.ALL) {

            val furthestIndex = count - daysAgo.numDays
            val furthestDate = dailyData[furthestIndex]

            val topValue: Float = when(value) {
                Value.RECOVERED -> furthestDate.recovered.toFloat()
                Value.CONFIRMED -> furthestDate.confirmed.toFloat()
                Value.DEATH -> furthestDate.deaths.toFloat()
                else -> applyActiveBounds(furthestIndex, bounds)
            }

            bounds.left = (count - daysAgo.numDays).toFloat()

            if (value != Value.ACTIVE) {
                bounds.top = topValue
            }

            Log.i("CovidAdapter", "Height = ${bounds.height()}; Top = ${bounds.top}")
        }

        curMin = bounds.top
        Log.i("CovidAdapter", "curMin = $curMin")

        return bounds
    }

    private fun applyActiveBounds(furthestIndex: Int, bounds: RectF): Float {
        // Calculate minimum and maximum value of cases for range < furthest; newest >
        // Apply bounds to top and bottom
        val indexes = furthestIndex until count
        val activeValues = mutableListOf<Int>()

        for (i in indexes) {
            activeValues.add(dailyData[i].active)
        }

        val maxValue = activeValues.maxOrNull()
        val minValue = activeValues.minOrNull()

        bounds.top = minValue!!.toFloat()
        bounds.bottom = maxValue!!.toFloat()

        return minValue.toFloat()
    }
}
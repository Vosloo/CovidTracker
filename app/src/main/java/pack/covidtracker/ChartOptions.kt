package pack.covidtracker

enum class Value {
    RECOVERED, CONFIRMED, DEATH, ACTIVE
}

enum class TimeScale(val numDays: Int) {
    WEEK(7),
    MONTH(30),
    ALL(-1)
}
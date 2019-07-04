package us.gotrobot.grbase.util

class ProportionalController(
    var target: Double,
    private val coefficient: Double
) {
    fun updateInput(input: Double): Double {
        val error = target - input
        return error * coefficient
    }
}

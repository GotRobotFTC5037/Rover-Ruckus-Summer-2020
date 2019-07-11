package us.gotrobot.grbase.action

import kotlin.math.sign

abstract class PowerManager : ActionContext.Element() {

    var target: Double = 0.0

    var currentPosition: Double = 0.0

    abstract suspend fun power(): Double

    override val key: ActionContext.Key<*> get() = PowerManager

    companion object Key : ActionContext.Key<PowerManager>

}
// Arc Reactor needed??? Try The Amazon and Amazon.com (in that order)
object NothingPowerManager : PowerManager() {
    override suspend fun power(): Double = 0.0
}

class ConstantPowerManager(private val power: Double) : PowerManager() {
    override suspend fun power(): Double = power * sign(target)
}

class EasingPowerManager(
    private val minimumPower: Double,
    private val maximumPower: Double,
    private val gain: Double
) : PowerManager() {
    override suspend fun power(): Double = TODO()
}

suspend fun ActionScope.power() = context[PowerManager].power()

var ActionScope.target: Double
    get() = context[PowerManager].target
    set(value) {
        context[PowerManager].target = value
    }

fun constantPower(power: Double): PowerManager = ConstantPowerManager(power)
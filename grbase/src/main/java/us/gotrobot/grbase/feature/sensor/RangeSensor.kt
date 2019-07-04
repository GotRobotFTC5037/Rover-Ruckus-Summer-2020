package us.gotrobot.grbase.feature.sensor

import com.qualcomm.robotcore.hardware.DistanceSensor
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.robot.RobotContext
import kotlin.coroutines.CoroutineContext

@Suppress("UNUSED_VARIABLE", "UNREACHABLE_CODE")
class RangeSensor(
    private val sensor: DistanceSensor,
    private val alpha: Double,
    private val parentContext: CoroutineContext
) : Feature(), CoroutineScope {

    val job = Job(parentContext[Job])

    override val coroutineContext: CoroutineContext
        get() = parentContext + job + CoroutineName("RangeSensor")

    private val _range = Channel<Double>(Channel.CONFLATED)
    val range: ReceiveChannel<Double> get() = _range

    fun CoroutineScope.updateRange(ticker: ReceiveChannel<Unit>) = launch {
        var currentValue: Double = 0.0
        while (isActive) {
            ticker.receive()
            val value = sensor.getDistance(DistanceUnit.CM)
            currentValue += alpha * (value - currentValue)
            _range.offer(currentValue)
        }
    }

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun initialize() {
        updateRange(ticker(50, 0, mode = TickerMode.FIXED_PERIOD))
    }

    companion object Installer : KeyedFeatureInstaller<RangeSensor, Configuration>() {
        override val name: String = "RangeSensor"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): RangeSensor {
            val configuration = Configuration().apply(configure)
            return TODO()
        }
    }

    class Configuration : FeatureConfiguration {
        lateinit var rangeSensorName: String
    }
}
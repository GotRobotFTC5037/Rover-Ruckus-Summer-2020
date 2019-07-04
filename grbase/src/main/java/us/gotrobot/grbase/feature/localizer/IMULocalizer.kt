package us.gotrobot.grbase.feature.localizer

import com.qualcomm.hardware.bosch.BNO055IMU
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.AxesOrder
import org.firstinspires.ftc.robotcore.external.navigation.AxesReference
import org.firstinspires.ftc.robotcore.external.navigation.Orientation
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.robot.RobotContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

class IMULocalizer(
    private val imu: BNO055IMU,
    private val parentContext: CoroutineContext
) : Feature(), OrientationLocalizer, CoroutineScope {

    private val job: Job = Job(parentContext[Job])

    @Suppress("EXPERIMENTAL_API_USAGE")
    override val coroutineContext: CoroutineContext
        get() = parentContext + CoroutineName("IMU Localizer") + job + newSingleThreadContext("IMULocalizer")

    private val orientationChannel: Channel<Orientation> = Channel(Channel.CONFLATED)

    @Suppress("EXPERIMENTAL_API_USAGE")
    private val headingChannel: BroadcastChannel<Double> = BroadcastChannel(Channel.CONFLATED)

    @Suppress("EXPERIMENTAL_API_USAGE")
    private fun CoroutineScope.startUpdatingOrientation(ticker: ReceiveChannel<Unit>) = launch {
        while (isActive) {
            ticker.receive()
            val orientation = imu.getAngularOrientation(IMUAngularOrientationOptions())
            orientationChannel.send(orientation)
            headingChannel.send(orientation.firstAngle.toDouble())
        }
    }

    override suspend fun orientation() = orientationChannel.receive()

    @Suppress("EXPERIMENTAL_API_USAGE")
    override suspend fun headingChannel(): ReceiveChannel<Double> = headingChannel.openSubscription()

    companion object Installer : KeyedFeatureInstaller<IMULocalizer, Configuration>() {

        override val name: String = "IMU Localizer"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): IMULocalizer {
            val configuration = Configuration().apply(configure)
            val imu = context.hardwareMap.get(BNO055IMU::class.java, configuration.imuName)
            val parameters = BNO055IMU.Parameters().apply {
                angleUnit = BNO055IMU.AngleUnit.DEGREES
            }
            imu.initialize(parameters)
            return IMULocalizer(imu, coroutineContext).apply {
                @Suppress("EXPERIMENTAL_API_USAGE")
                startUpdatingOrientation(
                    ticker(
                        10,
                        0,
                        this.coroutineContext,
                        TickerMode.FIXED_PERIOD
                    )
                )
            }
        }

    }

    class Configuration : FeatureConfiguration {
        var imuName: String = "imu"
    }

}

data class IMUAngularOrientationOptions(
    val reference: AxesReference = AxesReference.INTRINSIC,
    val order: AxesOrder = AxesOrder.ZYX,
    val unit: AngleUnit = AngleUnit.DEGREES
)

fun BNO055IMU.getAngularOrientation(
    options: IMUAngularOrientationOptions
): Orientation {
    val (ref, order, unit) = options
    return getAngularOrientation(ref, order, unit)
}

suspend fun BNO055IMU.suspendUntilGyroCalibration() {
    while (!isGyroCalibrated) {
        yield()
    }
}


package us.gotrobot.grbase.feature

import kotlinx.coroutines.channels.ReceiveChannel
import us.gotrobot.grbase.feature.localizer.HeadingLocalizer
import us.gotrobot.grbase.feature.localizer.IMULocalizer
import us.gotrobot.grbase.robot.RobotContext
import kotlin.math.abs

class TargetHeading(
    private val headingChannel: ReceiveChannel<Double>,
    initialTargetHeading: Double = 0.0
) : Feature() {

    var targetHeading: Double = initialTargetHeading
        set(value) {
            field = properHeading(value)
        }

    suspend fun deltaFromHeading(heading: Double): Double {
        val currentHeading = headingChannel.receive()
        val delta = properHeading(heading) - currentHeading
        return if (abs(delta) <= abs(delta - 360)) delta
        else delta - 360
    }

    suspend fun deltaFromTarget() = deltaFromHeading(targetHeading)

    companion object Installer : KeyedFeatureInstaller<TargetHeading, Configuration>() {

        override val name: String = "Target Heading"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): TargetHeading {
            val config = Configuration().apply(configure)
            val headingLocalizer: HeadingLocalizer = featureSet[config.headingLocalizerKey]
            return TargetHeading(headingLocalizer.headingChannel())
        }
    }

    class Configuration : FeatureConfiguration {
        lateinit var headingLocalizerKey: FeatureKey<IMULocalizer>
    }

}

tailrec fun properHeading(heading: Double): Double = when {
    heading > 180.0 -> properHeading(heading - 360)
    heading <= -180.0 -> properHeading(heading + 360)
    else -> heading
}
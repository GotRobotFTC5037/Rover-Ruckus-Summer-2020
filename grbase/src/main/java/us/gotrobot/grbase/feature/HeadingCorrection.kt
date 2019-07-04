package us.gotrobot.grbase.feature

import us.gotrobot.grbase.action.ActionName
import us.gotrobot.grbase.action.action
import us.gotrobot.grbase.action.feature
import us.gotrobot.grbase.feature.drivetrain.InterceptableDriveTrain
import us.gotrobot.grbase.feature.drivetrain.MecanumDriveTrain
import us.gotrobot.grbase.feature.localizer.IMULocalizer
import us.gotrobot.grbase.robot.RobotContext

class HeadingCorrection : Feature() {

    var enabled: Boolean = false

    companion object Installer : KeyedFeatureInstaller<HeadingCorrection, Configuration>() {

        override val name: String = "Heading Correction"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): HeadingCorrection {
            val driveTrain: InterceptableDriveTrain<*> = featureSet[MecanumDriveTrain]
            val targetHeading = featureSet[TargetHeading]
            val localizer = featureSet[IMULocalizer]

            val configuration = Configuration().apply(configure)
            val coefficient = configuration.coefficient

            val headingCorrection = HeadingCorrection()

            val headingChannel = localizer.headingChannel()

            driveTrain.powerPipeline.intercept {
                if (headingCorrection.enabled) {
                    val current = headingChannel.receive()
                    val target = targetHeading.targetHeading
                    val error = target - current
                    val adjustment = error * coefficient
                    subject.adjustHeadingPower(adjustment)
                }
                proceed()
            }
            return headingCorrection
        }

    }

    class Configuration : FeatureConfiguration {
        var coefficient: Double = 0.0
    }
}

fun toggleHeadingCorrection() = action {
    val correction = feature(HeadingCorrection)
    correction.enabled = !correction.enabled
}.apply {
    context.add(ActionName("Toggle Heading Correction"))
}

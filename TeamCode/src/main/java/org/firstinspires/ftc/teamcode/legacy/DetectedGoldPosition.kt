package org.firstinspires.ftc.teamcode.legacy

import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.robot.RobotContext

class DetectedGoldPosition : Feature() {

    var detectedGoldPosition = CargoDetector.GoldPosition.UNKNOWN

    companion object Installer : KeyedFeatureInstaller<DetectedGoldPosition, Configuration>() {
        override val name: String = "DetectedGoldPosition"
        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): DetectedGoldPosition = DetectedGoldPosition()
    }

    class Configuration : FeatureConfiguration

}
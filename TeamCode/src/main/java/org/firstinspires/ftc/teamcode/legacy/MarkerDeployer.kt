package org.firstinspires.ftc.teamcode.legacy

import com.qualcomm.robotcore.hardware.ServoImplEx
import us.gotrobot.grbase.action.ActionName
import us.gotrobot.grbase.action.ActionScope
import us.gotrobot.grbase.action.action
import us.gotrobot.grbase.action.feature
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.robot.RobotContext
import us.gotrobot.grbase.util.get

class MarkerDeployer(private val servo: ServoImplEx) : Feature() {

    var isDeployed: Boolean = false
        private set

    fun attach() {
        servo.position = AttachPosition
        isDeployed = false
    }

    fun deploy() {
        servo.position = DeployPosition
        isDeployed = true
    }

    fun toggle() = if (isDeployed) attach() else deploy()

    companion object Installer : KeyedFeatureInstaller<MarkerDeployer, Configuration>() {

        const val AttachPosition = 0.65
        const val DeployPosition = 0.175

        override val name: String = "Marker Deployer"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): MarkerDeployer {
            val configuration = Configuration().apply(configure)
            val servo = context.hardwareMap[ServoImplEx::class, configuration.servoName]
            return MarkerDeployer(servo).apply { attach() }
        }

    }

    class Configuration : FeatureConfiguration {
        lateinit var servoName: String
    }
}

val ActionScope.markerDeployer get() = feature(MarkerDeployer)

fun releaseMarker() = action {
    markerDeployer.deploy()
}.apply {
    context.add(ActionName("Release Marker"))
}

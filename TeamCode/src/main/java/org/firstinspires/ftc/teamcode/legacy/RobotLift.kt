package org.firstinspires.ftc.teamcode.legacy

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.TouchSensor
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
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

class RobotLift(
    private val liftMotor: DcMotorEx,
    private val limitSwitch: TouchSensor
) : Feature() {

    val isLowered get() = limitSwitch.isPressed

    val position get() = liftMotor.currentPosition

    fun setLiftMotorPower(power: Double) {
        liftMotor.power = power
    }

    companion object Installer : KeyedFeatureInstaller<RobotLift, Configuration>() {

        const val MAX_POSITION: Int = 19_100

        override val name: String = "Lift"
        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): RobotLift {
            val (liftMotorName, limitSwitchName) = Configuration().apply(configure)
            val liftMotor = context.hardwareMap[DcMotorEx::class, liftMotorName].apply {
                mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                delay(100)
                mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
            }
            val limitSwitch = context.hardwareMap[TouchSensor::class, limitSwitchName]
            return RobotLift(liftMotor, limitSwitch)
        }
    }

    class Configuration : FeatureConfiguration {
        lateinit var liftMotorName: String
        lateinit var limitSwitchName: String
        operator fun component1() = liftMotorName
        operator fun component2() = limitSwitchName
    }

}

val ActionScope.robotLift get() = feature(RobotLift)

fun lowerLift() = action {
    robotLift.setLiftMotorPower(-1.0)
    while (!robotLift.isLowered) {
        yield()
    }
    robotLift.setLiftMotorPower(0.0)
}

fun extendLift() = action {
    robotLift.setLiftMotorPower(1.0)
    while (robotLift.position < RobotLift.MAX_POSITION) {
        yield()
    }
    robotLift.setLiftMotorPower(0.0)
}.apply {
    context.add(ActionName("Extend Lift"))
}
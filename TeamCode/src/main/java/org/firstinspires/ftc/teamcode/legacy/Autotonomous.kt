@file:Suppress("unused")

package org.firstinspires.ftc.teamcode.legacy

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.util.ElapsedTime
import kotlinx.coroutines.*
import us.gotrobot.grbase.action.*
import us.gotrobot.grbase.feature.toggleHeadingCorrection
import us.gotrobot.grbase.opmode.RobotOpMode
import us.gotrobot.grbase.robot.Robot
import us.gotrobot.grbase.robot.install
import us.gotrobot.grbase.robot.robot

fun detectGoldPosition(timeout: Long) = action {
    val cargoDetector = feature(CargoDetector)
    val detectedGoldPosition = feature(DetectedGoldPosition)
    detectedGoldPosition.detectedGoldPosition =
        withTimeoutOrNull(timeout) { cargoDetector.goldPosition.firstKnownPosition() }
            ?: CargoDetector.GoldPosition.UNKNOWN
    GlobalScope.launch {
        cargoDetector.shutdown()
    }
}

fun cargoConditionalAction(left: Action, center: Action, right: Action) = action {
    when (feature(DetectedGoldPosition).detectedGoldPosition) {
        CargoDetector.GoldPosition.LEFT -> perform(left)
        CargoDetector.GoldPosition.CENTER -> perform(center)
        CargoDetector.GoldPosition.RIGHT -> perform(right)
        else -> throw RuntimeException("This should never happen")
    }
}

fun wait(duration: Long) = action {
    val elapsedTime = ElapsedTime()
    while (isActive && elapsedTime.milliseconds() < duration) {
        yield()
    }
}

fun initialAutoAction() = actionSequenceOf(
    detectGoldPosition(GOLD_DETECTION_TIMEOUT),
    extendLift(),
    timeDrive(time = 200, power = 0.2),
    biasedLateralDrive(distance = 20.0, bias = 0.05) with constantPower(0.35),
    toggleHeadingCorrection(),
    linearDrive(distance = 44.5) with constantPower(0.35),
    cargoConditionalAction(
        left = actionSequenceOf(
            lateralDrive(distance = -80.0) with constantPower(0.50),//The first one for sample of the Rich $tuf
            linearDrive(distance = 30.0) with constantPower(0.35),
            linearDrive(distance = -30.0) with constantPower(0.35)
        ),
        center = actionSequenceOf(
            lateralDrive(-25.0) with constantPower(0.35),//The first one for sample of the Soda Pop
            linearDrive(distance = 40.0) with constantPower(0.35),
            linearDrive(-35.0) with constantPower(0.35)
        ),
        right = actionSequenceOf(
            lateralDrive(distance = 60.0) with constantPower(0.50),//The first one for sample of the ice cream
            linearDrive(distance = 15.0) with constantPower(0.35),
            linearDrive(distance = -20.0) with constantPower(0.35)
        )
    )
)

private const val GOLD_DETECTION_TIMEOUT = 1500L

@Autonomous(name = "Left Autonomous", group = "0_competitive")
class LeftAutonomous : RobotOpMode() {

    override val action: Action = actionSequenceOf(
        initialAutoAction(),
        cargoConditionalAction(
            left = lateralDrive(distance = -55.0),
            center = lateralDrive(distance = -150.0),
            right = lateralDrive(distance = -1.0)
        ),
        turnTo(heading = 135.0) with constantPower(0.35),
        linearDrive(30.0),
        lateralDrive(60.0),
        linearDrive(115.0),
        releaseMarker(),
        linearDrive(-175.0),
        driveForever(-0.25)

    )

    override suspend fun robot(): Robot = Metabot()

}

@Autonomous(name = "Right Autonomous", group = "0_competitive")
class RightAutonomous : RobotOpMode() {

    override val action: Action = actionSequenceOf(
        initialAutoAction(),
        cargoConditionalAction(
            left = actionSequenceOf(wait(500), lateralDrive(distance = 225.0)),
            center = actionSequenceOf(wait(500), lateralDrive(distance = 150.0)),
            right = actionSequenceOf(wait(500), lateralDrive(distance = 55.0))
        ),
        turnTo(heading = -135.0) with constantPower(0.35),
        linearDrive(30.0),
        lateralDrive(-30.0),
        linearDrive(100.0),
        releaseMarker(),
        linearDrive(-155.0),
        turnTo(-90.0) with constantPower(0.30),
        linearDrive(-300.0),
        turnTo(-45.0),
        lateralDrive(-45.0),
        driveForever(-0.25)
    )

    override suspend fun robot(): Robot = Metabot()

}

@Autonomous(name = "Back Autonomous", group = "0_competitive")
class BackAutonomous : RobotOpMode() {

    override val action: Action = actionSequenceOf(
        initialAutoAction(),
        cargoConditionalAction(
            left = actionSequenceOf(lateralDrive(-50.0), driveForever(0.30)),
            center = actionSequenceOf(lateralDrive(-150.1), driveForever(0.30)),
            right = actionSequenceOf(lateralDrive(-196.2), driveForever(0.30)))
        )

    override suspend fun robot(): Robot = Metabot()

}

@Autonomous(name = "Retract Lift", group = "1_tools")
class RetractLift : RobotOpMode() {
    override val action: Action = lowerLift()
    override suspend fun robot(): Robot = robot {
        install(RobotLift) {
            this.liftMotorName = Metabot.LIFT_MOTOR
            this.limitSwitchName = Metabot.LIMIT_SWITCH
        }
        install(MarkerDeployer) {
            this.servoName = Metabot.MARKER_DEPLOYER_SERVO
        }
    }
}
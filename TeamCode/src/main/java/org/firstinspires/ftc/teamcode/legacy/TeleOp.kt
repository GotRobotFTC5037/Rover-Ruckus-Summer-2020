package org.firstinspires.ftc.teamcode.legacy

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import us.gotrobot.grbase.action.*
import us.gotrobot.grbase.feature.HeadingCorrection
import us.gotrobot.grbase.feature.drivetrain.MecanumDriveTrain
import us.gotrobot.grbase.opmode.CoroutineOpMode
import us.gotrobot.grbase.robot.Robot
import us.gotrobot.grbase.robot.robot
import us.gotrobot.grbase.robot.install
import us.gotrobot.grbase.feature.drivetrain.MecanumDriveTrain.Localizer
import us.gotrobot.grbase.feature.drivetrain.mecanumDriveTrain

typealias DriverControl = TeleOp

@Suppress("unused", "SpellCheckingInspection")
@DriverControl(name = "TeleOp")
class TeleOp : CoroutineOpMode() {

    lateinit var robot: Robot

    private val Float.isPressed get() = this >= 0.5

    override suspend fun initialize() {
        robot = Metabot().apply {
            features[HeadingCorrection].enabled = false
        }
    }

    private fun ActionScope.loop(block: suspend () -> Unit) = this.launch {
        while (isActive) {
            block.invoke()
            yield()
        }
    }

    override suspend fun run() = robot.perform("TeleOp") {
        val driveTrain = feature(MecanumDriveTrain)
        val cargoDelivery = feature(CargoDeliverySystem)
        val markerDeployer = feature(MarkerDeployer)

        var reversed = false

        loop {
            val multiplyer =
                (if (gamepad1.left_stick_button) 1.0 else 0.65) * (if (reversed) 1 else -1)
            val linearPower = -gamepad1.left_stick_y.toDouble() * multiplyer
            val lateralPower = gamepad1.left_stick_x.toDouble() * multiplyer
            val rotationalPower = gamepad1.right_stick_x.toDouble() * 0.9
            driveTrain.setDirectionPower(linearPower, lateralPower, rotationalPower)
        }

        loop {
            if (gamepad1.start) {
                reversed = !reversed
                while (gamepad1.start) {
                    yield()
                }
            }
        }

//        loop {
//            if (gamepad1.dpad_up) {
//                perform(
//                    actionSequenceOf(
//                        linearDrive(-50.0),
//                        lateralDrive(15.0),
//                        turnTo(-45.0),
//                        linearDrive(-200.0),
//                        turnTo(45.0),
//                        linearDrive(-15.0)
//                    )
//                )
//
//            }
//            suspend fun robot(): Robot = Metabot()
//        }


        loop {
            when {
                gamepad1.y -> robotLift.setLiftMotorPower(1.0)
                gamepad1.a -> if (robotLift.isLowered.not()) robotLift.setLiftMotorPower(-1.0)
                else -> robotLift.setLiftMotorPower(0.0)
            }
        }

        loop {
            if (gamepad2.y) {
                markerDeployer.toggle()
                while (gamepad2.y) {
                    yield()
                }
            }
        }

        loop {
            when {
                gamepad2.dpad_left ->
                    cargoDelivery.setSortingDirection(CargoDeliverySystem.SortingDirection.LEFT)
                gamepad2.dpad_right ->
                    cargoDelivery.setSortingDirection(CargoDeliverySystem.SortingDirection.RIGHT)
            }
        }

        loop {
            when {
                gamepad2.a -> cargoDelivery.setIntakeStatus(CargoDeliverySystem.IntakeStatus.ADMIT)
                gamepad2.b -> cargoDelivery.setIntakeStatus(CargoDeliverySystem.IntakeStatus.EJECT)
                else -> cargoDelivery.setIntakeStatus(CargoDeliverySystem.IntakeStatus.STOPPED)
            }
        }

        var deliverJob = launch {}
        loop {
            if (gamepad2.left_trigger.isPressed || gamepad2.right_trigger.isPressed || -gamepad2.left_stick_y.toDouble() >= 0.0) {
                deliverJob.cancel()
            }
            if (!deliverJob.isActive) {
                when {
                    gamepad2.left_trigger > 0.5 -> cargoDelivery.setRotationPower(gamepad2.left_trigger.toDouble())
                    gamepad2.right_trigger > 0.5 -> cargoDelivery.setRotationPower(-gamepad2.right_trigger.toDouble())
                    else -> cargoDelivery.setRotationPower(0.0)
                }
                cargoDelivery.setExtensionPower(-gamepad2.left_stick_y.toDouble())
                when {
                    gamepad2.dpad_up -> deliverJob = launch {
                        cargoDelivery.setExtendtionPosition(1025)
                        cargoDelivery.setRotationPosition(1300)
                    }
                }

            }

        }

        coroutineContext[Job]!!.children.forEach { it.join() }
    }

}

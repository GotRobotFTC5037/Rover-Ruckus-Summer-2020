package us.gotrobot.grbase.action

import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.yield
import us.gotrobot.grbase.feature.HeadingCorrection
import us.gotrobot.grbase.feature.TargetHeading
import us.gotrobot.grbase.feature.drivetrain.LinearDriveTrain
import us.gotrobot.grbase.feature.drivetrain.MecanumDriveTrain
import us.gotrobot.grbase.feature.drivetrain.RotationalDriveTrain
import us.gotrobot.grbase.feature.drivetrain.mecanumDriveTrain

fun linearDrive(distance: Double): Action = action {
    val driveTrain = feature(LinearDriveTrain::class)
    val localizer = feature(MecanumDriveTrain.Localizer)

    val linearPositionChannel = localizer.linearPosition()
    target = distance

    if (distance > 0.0) {
        while (isActive && distance > linearPositionChannel.receive()) {
            driveTrain.setLinearPower(power())
        }
    } else if (distance < 0.0) {
        while (isActive && distance < linearPositionChannel.receive()) {
            driveTrain.setLinearPower(power())
        }
    }

    linearPositionChannel.cancel()
    driveTrain.setLinearPower(0.0)
}.apply {
    context.add(ActionName("Linear Drive"))
}

fun lateralDrive(distance: Double): Action = action {
    val driveTrain = feature(MecanumDriveTrain)
    val localizer = feature(MecanumDriveTrain.Localizer)

    val lateralPositionChannel = localizer.lateralPosition()
    target = distance

    if (distance > 0.0) {
        while (isActive && distance > lateralPositionChannel.receive()) {
            driveTrain.setLateralPower(power())
        }
    } else if (distance < 0.0) {
        while (isActive && distance < lateralPositionChannel.receive()) {
            driveTrain.setLateralPower(power())
        }
    }

    lateralPositionChannel.cancel()
    driveTrain.setLinearPower(0.0)
}.apply {
    context.add(ActionName("Lateral Drive"))
}

fun biasedLateralDrive(distance: Double, bias: Double) = action {
    val driveTrain = feature(MecanumDriveTrain)
    val localizer = feature(MecanumDriveTrain.Localizer)

    val lateralPositionChannel = localizer.lateralPosition()
    target = distance

    if (distance > 0.0) {
        while (isActive && distance > lateralPositionChannel.receive()) {
            driveTrain.setDirectionPower(bias, power(), 0.0)
        }
    } else if (distance < 0.0) {
        while (isActive && distance < lateralPositionChannel.receive()) {
            driveTrain.setDirectionPower(bias, power(), 0.0)
        }
    }

    lateralPositionChannel.cancel()
    driveTrain.setLinearPower(0.0)
}.apply {
    context.add(ActionName("Biased Lateral Drive"))
}

fun turnTo(heading: Double): Action = action {
    val driveTrain = feature(RotationalDriveTrain::class)
    val headingCorrection = feature(HeadingCorrection).apply { enabled = false }
    val targetHeading = feature(TargetHeading).apply { targetHeading = heading }

    val initialDelta = targetHeading.deltaFromHeading(heading)
    target = initialDelta

    if (initialDelta > 0) {
        while (isActive && targetHeading.deltaFromHeading(heading) > 0) {
            driveTrain.setRotationalPower(power())
        }
    } else if (initialDelta < 0) {
        while (isActive && targetHeading.deltaFromHeading(heading) < 0) {
            driveTrain.setRotationalPower(power())
        }
    }

    driveTrain.setRotationalPower(0.0)
    headingCorrection.enabled = true
}.apply {
    context.add(ActionName("Turn To"))
}

fun driveForever(power: Double): Action = action {
    val driveTrain = feature(LinearDriveTrain::class)
    while (isActive) {
        driveTrain.setLinearPower(power)
        yield()
    }
}.apply {
    context.add(ActionName("Drive Forever"))
}

fun timeDrive(time: Long, power: Double) = action {
    mecanumDriveTrain.setLinearPower(power)
    delay(time)
    mecanumDriveTrain.stop()
}.apply {
    context.add(ActionName("Time Drive"))
}
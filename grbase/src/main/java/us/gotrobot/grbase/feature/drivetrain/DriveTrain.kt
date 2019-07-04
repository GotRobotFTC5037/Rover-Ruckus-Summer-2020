package us.gotrobot.grbase.feature.drivetrain

import us.gotrobot.grbase.pipeline.Pipeline

interface DriveTrain {
    fun stop()
}

interface LinearDriveTrain : DriveTrain {
    fun setLinearPower(power: Double)
}

interface LateralDriveTrain : DriveTrain {
    fun setLateralPower(power: Double)
}

interface RotationalDriveTrain : DriveTrain {
    fun setRotationalPower(power: Double)
}

interface OmnidirectionalDriveTrain : LinearDriveTrain, LateralDriveTrain, RotationalDriveTrain {
    fun setDirectionPower(
        linearPower: Double,
        lateralPower: Double,
        rotationalPower: Double
    )
}

interface DriveTrainMotorPowers {
    fun adjustHeadingPower(power: Double)
}

@Suppress("SpellCheckingInspection")
interface InterceptableDriveTrain<T : DriveTrainMotorPowers> {
    val powerPipeline: Pipeline<T, DriveTrain>
}

//package us.gotrobot.grbase.feature.drivetrain
//
//import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
//import com.qualcomm.robotcore.hardware.DcMotor
//import com.qualcomm.robotcore.hardware.HardwareMap
//import kotlinx.coroutines.*
//import kotlinx.coroutines.channels.*
//import kotlinx.coroutines.selects.select
//import us.gotrobot.grbase.pipeline.Pipeline
//import us.gotrobot.grbase.feature.FeatureConfiguration
//import us.gotrobot.grbase.feature.FeatureInstaller
//import us.gotrobot.grbase.feature.localizer.RobotPositionLocalizer
//import us.gotrobot.grbase.robot.robot
//import us.gotrobot.grbase.robot.hardwareMap
//import us.gotrobot.grbase.util.delayUntilStart
//import us.gotrobot.grbase.util.sameOrNull
//import kotlin.coroutines.CoroutineContext
//
//typealias TankDriveTrainLocalizer = TankDriveTrain.LocalizerInstaller
//
//class TankDriveTrain(
//    private val leftMotors: List<DcMotor>,
//    private val rightMotors: List<DcMotor>,
//    override val coroutineContext: CoroutineContext
//) : DriveTrain, CoroutineScope {
//
//    private val motors get() = leftMotors + rightMotors
//
//    private val powerChannel: Channel<MotorPowers> = Channel(Channel.CONFLATED)
//
//    val powerPipeline: Pipeline<MotorPowers, TankDriveTrain> = Pipeline()
//
//    suspend fun setMotorPowers(powers: MotorPowers) {
//        powerChannel.send(powers)
//    }
//
//    fun CoroutineScope.startUpdatingMotorPowers(opMode: LinearOpMode, ticker: ReceiveChannel<Unit>) = launch {
//        opMode.delayUntilStart()
//        var currentTargetPowers = MotorPowers(0.0, 0.0)
//        while (isActive) {
//            select<Unit> {
//                powerChannel.onReceive {
//                    currentTargetPowers = it
//                }
//                ticker.onReceive {
//                    val motorPowers =
//                        powerPipeline.execute(currentTargetPowers.copy(), this@TankDriveTrain)
//                    for (motor in leftMotors) {
//                        motor.power = motorPowers.left
//                    }
//                    for (motor in rightMotors) {
//                        motor.power = motorPowers.right
//                    }
//                }
//            }
//        }
//    }
//
//    data class MotorPowers(
//        val left: Double,
//        val right: Double
//    )
//
//    override fun stop() {
//        runBlocking { setMotorPowers(MotorPowers(0.0, 0.0)) }
//    }
//
//    companion object Installer : FeatureInstaller<Configuration, TankDriveTrain> {
//        @ObsoleteCoroutinesApi
//        override fun install(robot: robot, configure: Configuration.() -> Unit): TankDriveTrain {
//            val configuration = Configuration(robot.hardwareMap).apply(configure)
//            for (motor in configuration.leftMotors + configuration.rightMotors) {
//                motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
//            }
//            return TankDriveTrain(
//                configuration.leftMotors,
//                configuration.rightMotors,
//                robot.coroutineContext
//            ).apply {
//                startUpdatingMotorPowers(robot.linearOpMode, ticker(10, mode = TickerMode.FIXED_DELAY))
//            }
//        }
//    }
//
//    class Configuration(private val hardwareMap: HardwareMap) : FeatureConfiguration {
//
//        val leftMotors: MutableList<DcMotor> = mutableListOf()
//        val rightMotors: MutableList<DcMotor> = mutableListOf()
//
//        fun addLeftMotor(name: String, direction: MotorDirection = MotorDirection.REVERSE) {
//            val motor = hardwareMap.get(DcMotor::class.java, name)
//            motor.direction = direction
//            leftMotors.add(motor)
//        }
//
//        fun addRightMotor(name: String, direction: MotorDirection = MotorDirection.FORWARD) {
//            val motor = hardwareMap.get(DcMotor::class.java, name)
//            motor.direction = direction
//            rightMotors.add(motor)
//        }
//
//    }
//
//    /**
//     * The [PositionLocalizer] for localizing a [TankDriveTrain].
//     */
//    object LocalizerInstaller : FeatureInstaller<LocalizerConfiguration, PositionLocalizer> {
//        override fun install(
//            robot: robot,
//            configure: LocalizerConfiguration.() -> Unit
//        ): PositionLocalizer {
//            val configuration = LocalizerConfiguration().apply(configure)
//            val tankDrive = robot[TankDriveTrain]
//            val ticksPerRev =
//                tankDrive.motors.map { it.motorType.ticksPerRev }.sameOrNull() ?: TODO()
//            return tankDrive.PositionLocalizer(
//                configuration.wheelDiameter,
//                configuration.gearRatio,
//                ticksPerRev,
//                robot.coroutineContext
//            )
//        }
//    }
//
//    class LocalizerConfiguration : FeatureConfiguration {
//        var wheelDiameter = 1.0
//        var gearRatio = 1.0
//    }
//
//    data class LocalizerUpdate(
//        val leftPosition: Double,
//        val rightPosition: Double
//    ) {
//        val average: Double
//            get() = if (leftPosition > 5.0 && rightPosition > 5.0) {
//                (leftPosition + rightPosition) / 2
//            } else if (leftPosition > 5.0 && rightPosition <= 5.0) {
//                leftPosition
//            } else {
//                rightPosition
//            }
//    }
//
//    inner class PositionLocalizer(
//        wheelDiameter: Double,
//        private val gearRatio: Double,
//        private val ticksPerRevolution: Double,
//        override val coroutineContext: CoroutineContext
//    ) : RobotPositionLocalizer, CoroutineScope {
//
//        private val wheelCircumference = wheelDiameter * Math.PI
//
//        private fun DcMotor.currentDistance() =
//            currentPosition * wheelCircumference * gearRatio / ticksPerRevolution
//
//        private fun List<DcMotor>.averageDistance(): Double =
//            sumByDouble { it.currentDistance() } / count()
//
//        @Suppress("EXPERIMENTAL_API_USAGE")
//        fun CoroutineScope.producePosition() = produce(capacity = Channel.CONFLATED) {
//            val initialUpdate = LocalizerUpdate(
//                leftMotors.averageDistance(),
//                rightMotors.averageDistance()
//            )
//            while (true) {
//                val update = LocalizerUpdate(
//                    leftMotors.averageDistance() - initialUpdate.leftPosition,
//                    rightMotors.averageDistance() - initialUpdate.rightPosition
//                )
//                send(update)
//                yield()
//            }
//        }
//
//        fun newPositionChannel() = producePosition()
//
//    }
//
//}

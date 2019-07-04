package us.gotrobot.grbase.feature.drivetrain

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.selects.select
import us.gotrobot.grbase.action.ActionScope
import us.gotrobot.grbase.action.feature
import us.gotrobot.grbase.feature.Feature
import us.gotrobot.grbase.feature.FeatureConfiguration
import us.gotrobot.grbase.feature.FeatureSet
import us.gotrobot.grbase.feature.KeyedFeatureInstaller
import us.gotrobot.grbase.pipeline.Pipeline
import us.gotrobot.grbase.robot.RobotContext
import us.gotrobot.grbase.util.get
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.coroutineContext
import kotlin.math.PI

class MecanumDriveTrain(
    private val frontLeftMotor: DcMotorEx,
    private val frontRightMotor: DcMotorEx,
    private val backLeftMotor: DcMotorEx,
    private val backRightMotor: DcMotorEx,
    private val parentContext: CoroutineContext = EmptyCoroutineContext
) : Feature(),
    OmnidirectionalDriveTrain,
    InterceptableDriveTrain<MecanumDriveTrain.MotorMagnitudes>,
    CoroutineScope {

    private val job: Job = Job(parentContext[Job])

    @Suppress("EXPERIMENTAL_API_USAGE")
    override val coroutineContext: CoroutineContext
        get() = parentContext + CoroutineName("Mecanum Drive Train") + job + newSingleThreadContext("Drive Train")

    private val powerChannel: Channel<MotorMagnitudes> = Channel(Channel.CONFLATED)

    override val powerPipeline = Pipeline<MotorMagnitudes, DriveTrain>()

    data class MotorMagnitudes(
        var frontLeft: Double = 0.0,
        var frontRight: Double = 0.0,
        var backLeft: Double = 0.0,
        var backRight: Double = 0.0
    ) : DriveTrainMotorPowers {
        override fun adjustHeadingPower(power: Double) {
            frontLeft -= power
            frontRight += power
            backLeft -= power
            backRight += power
        }

        operator fun minus(other: MotorMagnitudes): MotorMagnitudes = MotorMagnitudes(
            this.frontLeft - other.frontLeft,
            this.frontRight - other.frontRight,
            this.backLeft - other.backLeft,
            this.backRight - other.backRight
        )

        fun sum(): Double = frontLeft + frontRight + backLeft + backRight

    }

    override fun setLinearPower(power: Double) {
        powerChannel.offer(MotorMagnitudes(power, power, power, power))
    }

    override fun setLateralPower(power: Double) {
        powerChannel.offer(MotorMagnitudes(power, -power, -power, power))
    }

    override fun setRotationalPower(power: Double) {
        powerChannel.offer(MotorMagnitudes(-power, power, -power, power))
    }

    override fun setDirectionPower(
        linearPower: Double,
        lateralPower: Double,
        rotationalPower: Double
    ) {
        powerChannel.offer(
            MotorMagnitudes(
                frontLeft = linearPower + lateralPower + rotationalPower,
                frontRight = linearPower - lateralPower - rotationalPower,
                backLeft = linearPower - lateralPower + rotationalPower,
                backRight = linearPower + lateralPower - rotationalPower
            )
        )
    }

    override fun stop() {
        powerChannel.offer(MotorMagnitudes())
    }

    private fun CoroutineScope.startUpdatingPowers(ticker: ReceiveChannel<Unit>) = launch {
        var targetPowers = MotorMagnitudes()
        while (isActive) {
            select<Unit> {
                powerChannel.onReceive {
                    targetPowers = it
                }
                ticker.onReceive {
                    val (fl, fr, bl, br) = powerPipeline.execute(
                        targetPowers, this@MecanumDriveTrain
                    )
                    frontLeftMotor.power = fl
                    frontRightMotor.power = fr
                    backLeftMotor.power = bl
                    backRightMotor.power = br
                }
            }
        }
    }

    companion object Installer : KeyedFeatureInstaller<MecanumDriveTrain, Configuration>() {

        override val name: String = "Mecanum Drive Train"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): MecanumDriveTrain {
            val (fl, fr, bl, br) = Configuration().apply(configure)
            val hm = context.hardwareMap
            val frontLeft = hm[DcMotorEx::class, fl].apply { setup(); setRevered() }
            val frontRight = hm[DcMotorEx::class, fr].apply { setup() }
            val backLeft = hm[DcMotorEx::class, bl].apply { setup(); setRevered() }
            val backRight = hm[DcMotorEx::class, br].apply { setup() }
            return MecanumDriveTrain(
                frontLeft, frontRight, backLeft, backRight, coroutineContext
            ).apply {
                @Suppress("EXPERIMENTAL_API_USAGE")
                startUpdatingPowers(ticker(10, 0, coroutineContext, TickerMode.FIXED_DELAY))
            }
        }

        private fun DcMotor.setup() {
            this.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            this.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        }

        private fun DcMotor.setRevered() {
            direction = DcMotorSimple.Direction.REVERSE
        }

    }

    class Configuration : FeatureConfiguration {
        var frontLeftMotorName: String = "front left motor"
        var frontRightMotorName: String = "front right motor"
        var backLeftMotorName: String = "back left motor"
        var backRightMotorName: String = "back right motor"
        operator fun component1() = frontLeftMotorName
        operator fun component2() = frontRightMotorName
        operator fun component3() = backLeftMotorName
        operator fun component4() = backRightMotorName
    }

    /* Localizer */

    inner class LocalizerFeature(
        private val wheelDiameter: Double,
        private val gearRatio: Double
    ) : Feature() {

        private val velocityChannel = Channel<MotorMagnitudes>(Channel.CONFLATED)

        private val ticksPerRevolution: Double by lazy {
            frontLeftMotor.motorType.ticksPerRev
        }

        private val unitConversionMultiplier: Double by lazy {
            gearRatio * wheelDiameter * PI / ticksPerRevolution
        }

        private fun CoroutineScope.startUpdatingPositions(ticker: ReceiveChannel<Unit>) = launch {
            var lastMotorPositions = MotorMagnitudes(
                frontLeftMotor.currentUnitPosition,
                frontRightMotor.currentUnitPosition,
                backLeftMotor.currentUnitPosition,
                backRightMotor.currentUnitPosition
            )
            while (isActive) {
                ticker.receive()
                val positions = MotorMagnitudes(
                    frontLeftMotor.currentUnitPosition,
                    frontRightMotor.currentUnitPosition,
                    backLeftMotor.currentUnitPosition,
                    backRightMotor.currentUnitPosition
                )
                val delta = positions - lastMotorPositions
                velocityChannel.send(delta)
                lastMotorPositions = positions
            }
        }

        fun startPositionUpdates(ticker: ReceiveChannel<Unit>) {
            startUpdatingPositions(ticker)
        }

        private val DcMotor.currentUnitPosition: Double
            get() = currentPosition * unitConversionMultiplier

        private suspend fun linearVelocity(): Double = velocityChannel.receive().linear()
        private suspend fun lateralVelocity(): Double = velocityChannel.receive().lateral()

        @Suppress("EXPERIMENTAL_API_USAGE")
        fun CoroutineScope.newLinearPositionChannel() = produce {
            var currentPosition = 0.0
            while (isActive) {
                currentPosition += linearVelocity()
                send(currentPosition)
            }
        }

        @Suppress("EXPERIMENTAL_API_USAGE")
        fun CoroutineScope.newLateralPositionChannel() = produce {
            var currentPosition = 0.0
            while (isActive) {
                currentPosition += lateralVelocity()
                send(currentPosition)
            }
        }

        fun linearPosition() = newLinearPositionChannel()
        fun lateralPosition() = newLateralPositionChannel()

        private fun MotorMagnitudes.linear(): Double = this.sum() * 0.25
        private fun MotorMagnitudes.lateral(): Double =
            (frontLeft + backRight - backLeft - frontRight) * 0.25 * 0.80

    }

    object Localizer : KeyedFeatureInstaller<LocalizerFeature, LocalizerConfiguration>() {

        override val name: String = "Mecanum Drive Train Localizer"

        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: LocalizerConfiguration.() -> Unit
        ): LocalizerFeature {
            val driveTrain = featureSet[MecanumDriveTrain]
            val configuration = LocalizerConfiguration().apply(configure)
            return driveTrain.LocalizerFeature(
                wheelDiameter = configuration.wheelDiameter,
                gearRatio = configuration.gearRatio
            ).apply {
                @Suppress("EXPERIMENTAL_API_USAGE")
                startPositionUpdates(ticker(10, 0, coroutineContext, TickerMode.FIXED_PERIOD))
            }
        }

    }

    class LocalizerConfiguration : FeatureConfiguration {
        var wheelDiameter: Double = 0.0
        var gearRatio: Double = 0.0
    }

}

val ActionScope.mecanumDriveTrain get() = feature(MecanumDriveTrain)
package us.gotrobot.grbase.feature

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.TickerMode
import kotlinx.coroutines.channels.ticker
import kotlinx.coroutines.selects.select
import us.gotrobot.grbase.robot.RobotContext
import us.gotrobot.grbase.util.get
import kotlin.coroutines.CoroutineContext

class ManagedMotor(
    private val motor: DcMotorEx,
    private val adjustmentCoefficient: Double,
    private val adjustmentDelay: Long,
    parentContext: CoroutineContext
) : Feature(), CoroutineScope {

    private val job = Job(parentContext[Job])

    override val coroutineContext: CoroutineContext =
        parentContext + job + CoroutineName("ManagedMotor")

    private val powerChannel = Channel<Double>(Channel.CONFLATED)
    private val positionChannel = Channel<Int>(Channel.CONFLATED)

    var positionRange: IntRange = Int.MIN_VALUE..Int.MAX_VALUE

    @Suppress("EXPERIMENTAL_API_USAGE")
    fun init() {
        val ticker = ticker(100, 0, mode = TickerMode.FIXED_DELAY)
        startUpdatingMotorPowers(ticker)
    }

    private fun CoroutineScope.startUpdatingMotorPowers(ticker: ReceiveChannel<Unit>) = launch {
        var outputPower = 0.0
        var lastDesiredPower = 0.0
        var currentPosition = 0
        var targetPosition = 0
        var holdPosition: Boolean = true
        while (isActive) {
            select<Unit> {
                ticker.onReceive {
                    motor.power = outputPower
                    currentPosition = motor.currentPosition
                    positionChannel.offer(currentPosition)
                }
                powerChannel.onReceive { desiredPower ->
                    if (desiredPower == 0.0) {
                        if (lastDesiredPower != 0.0) {
                            launch {
                                delay(adjustmentDelay)
                                targetPosition = motor.currentPosition
                                holdPosition = true
                            }
                        }
                    } else {
                        holdPosition = false
                    }
                    val adjustmentPower = if (holdPosition)
                        (targetPosition - currentPosition) * adjustmentCoefficient else 0.0
                    outputPower = when {
                        desiredPower < 0.0 && currentPosition > positionRange.start -> desiredPower
                        desiredPower > 0.0 && currentPosition < positionRange.endInclusive -> desiredPower
                        else -> 0.0
                    } + adjustmentPower
                    lastDesiredPower = desiredPower
                }
            }
        }
    }

    fun setPower(power: Double) {
        powerChannel.offer(power)
    }

    suspend fun setPosition(targetPosition: Int) {
        val currentPosition = positionChannel.receive()
        if (currentPosition > targetPosition) {
            while (positionChannel.receive() > targetPosition) {
                setPower(-1.0)
                yield()
            }
        } else if (currentPosition < targetPosition) {
            while (positionChannel.receive() < targetPosition) {
                setPower(1.0)
                yield()
            }
        }
        setPower(0.0)
    }

    companion object Installer : FeatureInstaller<ManagedMotor, Configuration>() {
        override val name: String = "Managed Motor"
        override suspend fun install(
            context: RobotContext,
            featureSet: FeatureSet,
            configure: Configuration.() -> Unit
        ): ManagedMotor {
            val configuration = Configuration().apply(configure)
            val motor = context.hardwareMap[DcMotorEx::class, configuration.name].apply {
                direction = configuration.direction
                zeroPowerBehavior = configuration.zeroPowerBehavior
                mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                delay(1000)
                mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
            }
            return ManagedMotor(
                motor,
                configuration.coefficient,
                configuration.adjustmentDelay,
                context.coroutineScope.coroutineContext
            ).apply {
                init()
            }
        }
    }

    class Configuration : FeatureConfiguration {
        lateinit var name: String
        var direction: DcMotorSimple.Direction = DcMotorSimple.Direction.FORWARD
        var zeroPowerBehavior: DcMotor.ZeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        var coefficient: Double = 0.0
        var adjustmentDelay = -1L
    }

}